package com.njlabs.showjava.processor;

import com.google.firebase.crash.*;
import com.njlabs.showjava.utils.*;
import com.njlabs.showjava.utils.logging.*;

import org.benf.cfr.reader.*;
import org.benf.cfr.reader.state.*;
import org.benf.cfr.reader.util.getopt.*;
import org.benf.cfr.reader.util.output.*;
import org.jetbrains.java.decompiler.main.decompiler.*;

import java.io.*;
import java.util.*;

import jadx.api.*;

/**
 * Created by Niranjan on 29-05-2015.
 */
public class JavaExtractor extends ProcessServiceHelper {

    public JavaExtractor(ProcessService processService) {
        this.processService = processService;
        this.UIHandler = processService.UIHandler;
        this.packageFilePath = processService.packageFilePath;
        this.packageName = processService.packageName;
        this.exceptionHandler = processService.exceptionHandler;
        this.sourceOutputDir = processService.sourceOutputDir;
        this.javaSourceOutputDir = processService.javaSourceOutputDir;

        if(printStream == null) {
            printStream = new PrintStream(new ProgressStream());
            System.setErr(printStream);
            System.setOut(printStream);
        }
    }

    public void extract() {

        broadcastStatus("jar2java");

        File dexInputFile = new File(sourceOutputDir + "/optimised_classes.dex");
        File jarInputFile = new File(sourceOutputDir + "/" + packageName + ".jar");

        final File javaOutputDir = new File(javaSourceOutputDir);

        if (!javaOutputDir.isDirectory()) {
            javaOutputDir.mkdirs();
        }

        if(!processService.decompilerToUse.equals("jadx")){
            if(dexInputFile.exists() && dexInputFile.isFile()){
                dexInputFile.delete();
            }
        }

        switch (processService.decompilerToUse) {
            case "jadx":
                decompileWithJaDX(dexInputFile, javaOutputDir);
                break;

            case "cfr":
                decompileWithCFR(jarInputFile, javaOutputDir);
                break;

            case "fernflower":
                decompileWithFernFlower(jarInputFile, javaOutputDir);
                break;

        }

    }

    private void decompileWithCFR(File jarInputFile, File javaOutputDir){
        String[] args = {jarInputFile.toString(), "--outputdir", javaOutputDir.toString()};
        GetOptParser getOptParser = new GetOptParser();

        Options options = null;
        try {
            options = getOptParser.parse(args, OptionsImpl.getFactory());

            if(!options.optionIsSet(OptionsImpl.HELP) && options.getOption(OptionsImpl.FILENAME) != null) {
                ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
                final DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
                final String path = options.getOption(OptionsImpl.FILENAME);
                final DumperFactory dumperFactory = new DumperFactoryImpl();

                ThreadGroup group = new ThreadGroup("Jar 2 Java Group");
                Thread javaExtractionThread = new Thread(group, () -> {
                    boolean javaError = false;
                    try {
                        Main.doJar(dcCommonState, path, dumperFactory);
                    } catch (Exception | StackOverflowError e) {
                        Ln.e(e);
                        javaError = true;
                    }
                    startXMLExtractor(!javaError);
                }, "Jar to Java Thread", processService.STACK_SIZE);

                javaExtractionThread.setPriority(Thread.MAX_PRIORITY);
                javaExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
                javaExtractionThread.start();

            } else {
                broadcastStatus("exit_process_on_error");
            }

        } catch (Exception e) {
            FirebaseCrash.report(e);
            broadcastStatus("exit_process_on_error");
        }

    }

    private void decompileWithJaDX(final File dexInputFile, final File javaOutputDir){

        ThreadGroup group = new ThreadGroup("Jar 2 Java Group");
        Thread javaExtractionThread = new Thread(group, () -> {
            boolean javaError = false;
            try {
                JadxDecompiler jadx = new JadxDecompiler();
                jadx.setOutputDir(javaOutputDir);
                jadx.loadFile(dexInputFile);
                jadx.saveSources();
            } catch (Exception | StackOverflowError e) {
                Ln.e(e);
                javaError = true;
            }
            if(dexInputFile.exists() && dexInputFile.isFile()){
                dexInputFile.delete();
            }
            startXMLExtractor(!javaError);
        }, "Jar to Java Thread", processService.STACK_SIZE);

        javaExtractionThread.setPriority(Thread.MAX_PRIORITY);
        javaExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        javaExtractionThread.start();
    }

    private void decompileWithFernFlower(final File jarInputFile, final File javaOutputDir){

        ThreadGroup group = new ThreadGroup("Jar 2 Java Group");
        Thread javaExtractionThread = new Thread(group, () -> {
            boolean javaError = false;
            try {

                final Map<String, Object> mapOptions = new HashMap<>();
                PrintStreamLogger logger = new PrintStreamLogger(printStream);
                ConsoleDecompiler decompiler = new ConsoleDecompiler(javaOutputDir, mapOptions, logger);
                decompiler.addSpace(jarInputFile, true);
                decompiler.decompileContext();

                File decompiledJarFile = new File(javaOutputDir + "/" + packageName +".jar");

                if(decompiledJarFile.exists()) {
                    ZipUtils.unzip(decompiledJarFile, javaOutputDir, printStream);
                    decompiledJarFile.delete();
                } else {
                    javaError = true;
                }

            } catch (Exception | StackOverflowError e) {
                Ln.e(e);
                javaError = true;
            }
            startXMLExtractor(!javaError);
        }, "Jar to Java Thread", processService.STACK_SIZE);

        javaExtractionThread.setPriority(Thread.MAX_PRIORITY);
        javaExtractionThread.setUncaughtExceptionHandler(exceptionHandler);
        javaExtractionThread.start();

    }

    private void startXMLExtractor(boolean hasJava) {
        SourceInfo.setjavaSourceStatus(processService, hasJava);
        ((new ResourcesExtractor(processService))).extract();
    }
}
