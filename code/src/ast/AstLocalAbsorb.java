package ast;

import java.util.ArrayList;
import java.util.Arrays;

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
		public boolean transformVisit = false;
	}
	private AstReducer reducerMain;
	private AstVisitedMap<AbsorbStat> visitInfo;
	public int statA,statB;

	public AstLocalAbsorb(AstNode root,AstReducer reducerMain)
	{
		visitInfo = new AstVisitedMap<AbsorbStat>();
		statA=statB=0;
		initShared(root);
		initAbsorbStat(root);
//		transform(root);
	}
	private void initShared(AstNode node)
	{
		if(visitInfo.isVisited(node))
		{	
			visitInfo.valueAt(node).shared=true;
			return;
		}
		visitInfo.visit(node,new AbsorbStat());
		AstNode[] child = node.children();
		for(int i=0;i<child.length;++i) initShared(child[i]);
	}
	private AbsorbStat initAbsorbStat(AstNode node)
	{
		AbsorbStat info = visitInfo.valueAt(node);
		if(info.absorbMarked) 
		{	assert info.shared;
			assert !node.needsGarbled() || !info.absorbsA;
			assert !node.needsGarbled() || !info.absorbsB;
			return info;
		}
		info.absorbMarked=true;
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

	private void transform(AstNode node)
	{
		if(visitInfo.isVisited(node) &&
				visitInfo.valueAt(node).transformVisit) return;
		if(!visitInfo.isVisited(node)) visitInfo.visit(node,new AbsorbStat());
		visitInfo.valueAt(node).transformVisit=true;
		tryAbsorb(node);
		AstNode[] child = node.children();
		for(int i=0;i<child.length;++i) transform(child[i]);
	}

	private void tryAbsorb(AstNode node)
	{
		if(node.getType()!=AstAddNode.class) return;

		int adump=-1,bdump=-1,i;
		AstNode[] child = node.children();
		for(i=0;i<child.length;++i)
		{	AbsorbStat s = visitInfo.valueAt(child[i]);
			if(s.absorbsA) adump=i;
			if(s.absorbsB) bdump=i;
		}
		if(adump==-1 && bdump==-1) return;
		ArrayList<AstNode> justA=new ArrayList<AstNode>();
		ArrayList<AstNode> justB=new ArrayList<AstNode>();
		ArrayList<AstNode> both =new ArrayList<AstNode>();
		for(i=0;i<child.length;++i)
		{	if(child[i].needsGarbled()) 
			{	assert child[i]!=null;
				both.add(child[i]);
			}
			else if(child[i].dependsOnA()){ if(i!=adump) justA.add(child[i]); }
			else if(child[i].dependsOnB()){ if(i!=adump) justB.add(child[i]); }
			else if(adump!=-1 && i!=adump) justA.add(child[i]);
			else if(bdump!=-1 && i!=bdump) justB.add(child[i]);
		}
		if((justA.size()==0 || adump==-1) && (justB.size()==0 || bdump==-1))
			return;
		if(justA.size()>0 && adump!=-1) 
			both.add(shoveInto(child[adump],justA));
		assert both.size()==0 || both.get(both.size()-1)!=null;
		if(justB.size()>0 && bdump!=-1)
		{	AstNode t = shoveInto(child[bdump],justB);
			if(adump!=bdump) both.add(t);
			else child[bdump]=t;
		}
		assert both.size()!=0: child.length+" "+adump+" "+bdump
			+" "+child[1].dependsOnB();
		if(both.size()==1) node.setData(both.get(0).getData());
		else node.setData(
				AstReducer.newSameType(node,both.toArray(new AstNode[0]))
				.getData());
	}
	// Example of codes I produce at 3 am
	private AstNode shoveInto(AstNode node,ArrayList<AstNode> fromAbove)
	{
		AstNode rv;
		if(node.children().length==0)
		{	AstNode[] addchild = new AstNode[fromAbove.size()+1];
			for(int i=0;i<fromAbove.size();++i) addchild[i]=fromAbove.get(i);
			addchild[fromAbove.size()]=node;
			rv=AstAddNode.create(addchild);
		}
		else if(node.getType()==AstAddNode.class)
		{	if(!node.needsGarbled()) 
			{	fromAbove.addAll(Arrays.asList(node));
				rv=AstAddNode.create(fromAbove.toArray(new AstNode[0]));
			}else
			{	boolean alice = !fromAbove.get(0).dependsOnB();
				AstNode[] copy = new AstNode[node.children().length];
				
				for(int i=0;i<node.children().length;++i)
				{	if(alice && !node.children()[i].dependsOnB())
						copy[i]=shoveInto(node.children()[i],fromAbove);
					else if(!alice && !node.children()[i].dependsOnA())
						copy[i]=shoveInto(node.children()[i],fromAbove);
					else copy[i]=fromAbove.get(i);
				}
				rv=AstAddNode.create(copy);
			}
		}
		else
		{
			// min or max at this point
			AstNode[] newchild = new AstNode[node.children().length];
			for(int i=0;i<node.children().length;++i)
				newchild[i]=shoveInto(node.children()[i],fromAbove);
			rv=AstReducer.newSameType(node,newchild);
		}
		AbsorbStat as = new AbsorbStat(), info = visitInfo.valueAt(node);
		as.absorbsA = info.absorbsA; as.absorbsB = info.absorbsB;
		visitInfo.visit(rv,as);
		return rv;
		
	}
}
