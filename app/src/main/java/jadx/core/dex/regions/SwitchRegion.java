package jadx.core.dex.regions;

import java.util.*;

import jadx.core.dex.nodes.*;

public final class SwitchRegion extends AbstractRegion implements IBranchRegion {

	private final BlockNode header;

	private final List<List<Object>> keys;
	private final List<IContainer> cases;
	private IContainer defCase;

	public SwitchRegion(IRegion parent, BlockNode header) {
		super(parent);
		this.header = header;
		this.keys = new ArrayList<List<Object>>();
		this.cases = new ArrayList<IContainer>();
	}

	public BlockNode getHeader() {
		return header;
	}

	public void addCase(List<Object> keysList, IContainer c) {
		keys.add(keysList);
		cases.add(c);
	}

	public void setDefaultCase(IContainer block) {
		defCase = block;
	}

	public IContainer getDefaultCase() {
		return defCase;
	}

	public List<List<Object>> getKeys() {
		return keys;
	}

	public List<IContainer> getCases() {
		return cases;
	}

	@Override
	public List<IContainer> getSubBlocks() {
		List<IContainer> all = new ArrayList<IContainer>(cases.size() + 2);
		all.add(header);
		all.addAll(cases);
		if (defCase != null) {
			all.add(defCase);
		}
		return Collections.unmodifiableList(all);
	}

	@Override
	public List<IContainer> getBranches() {
		List<IContainer> branches = new ArrayList<IContainer>(cases.size() + 1);
		branches.addAll(cases);
		branches.add(defCase);
		return Collections.unmodifiableList(branches);
	}

	@Override
	public String baseString() {
		return header.baseString();
	}

	@Override
	public String toString() {
		return "Switch: " + cases.size() + ", default: " + defCase;
	}
}
