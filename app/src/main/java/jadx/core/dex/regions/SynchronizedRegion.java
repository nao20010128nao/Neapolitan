package jadx.core.dex.regions;

import java.util.*;

import jadx.core.dex.nodes.*;

public final class SynchronizedRegion extends AbstractRegion {

	private final InsnNode enterInsn;
	private final List<InsnNode> exitInsns = new LinkedList<InsnNode>();
	private final Region region;

	public SynchronizedRegion(IRegion parent, InsnNode insn) {
		super(parent);
		this.enterInsn = insn;
		this.region = new Region(this);
	}

	public InsnNode getEnterInsn() {
		return enterInsn;
	}

	public List<InsnNode> getExitInsns() {
		return exitInsns;
	}

	public Region getRegion() {
		return region;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		return region.getSubBlocks();
	}

	@Override
	public String baseString() {
		return Integer.toHexString(enterInsn.getOffset());
	}

	@Override
	public String toString() {
		return "Synchronized:" + region;
	}
}
