package ast;

public class AstLocalAbsorb {
	public static class AbsorbStat {
		// These two are used for distributing adds to offload
		//   more to local computation. For example,
		//   add(min(x,b0),b1) --> min(add(x,b1),add(b0,b1)) doesn't
		//   help, but doesn't hurt either, since add(b0,b1) is local
		//   gets better if x = add(a,b2). Then it can remove an add.
		public boolean absorbsA = false, absorbsB = false;
		public boolean absorbMarked = false;
		public boolean shared = false;
	}
	private AstVisitedMap<AbsorbStat> visitInfo;
	public int statA,statB;

	public AstLocalAbsorb(AstNode root)
	{
		visitInfo = new AstVisitedMap<AbsorbStat>();
		statA=statB=0;
		initShared(root);
		initAbsorbStat(root);
	}
	private void initShared(AstNode node)
	{
		if(visitInfo.isVisited(node))
		{	visitInfo.valueAt(node).shared=true;
			return;
		}
		visitInfo.visit(node,new AbsorbStat());
		AstNode[] child = node.children();
		for(int i=0;i<child.length;++i) initShared(child[i]);
	}
	private AbsorbStat initAbsorbStat(AstNode node)
	{
		AbsorbStat info = visitInfo.valueAt(node);
		if(info.absorbMarked) return info;
		if(!node.needsGarbled())
		{	if(!node.dependsOnB()) info.absorbsA=true;
			if(!node.dependsOnA()) info.absorbsB=true;
			updateStats(info);
			return info;
		}
		AstNode[] child=node.children();
		if(child.length==0 || info.shared) return info;
		int ac=0,bc=0,i;
		for(i=0;i<child.length;++i)
		{	AbsorbStat childinfo = initAbsorbStat(child[i]);
			if(childinfo.absorbsA) ++ac;
			if(childinfo.absorbsB) ++bc;
		}
		if(node.getType()==AstAddNode.class)
		{	if(ac>0) info.absorbsA=true;
			if(bc>0) info.absorbsB=true;
		}else
		{	if(ac==child.length) info.absorbsA=true;
			if(bc==child.length) info.absorbsB=true;
		}
		updateStats(info);
		return info;
	}
	private void updateStats(AbsorbStat info)
	{	if(info.absorbsA==true) ++statA;
		if(info.absorbsB==true) ++statB;
	}
}
