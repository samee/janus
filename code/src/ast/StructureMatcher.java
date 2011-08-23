package ast;

import java.util.HashMap;

// Checks for the equality of the structure of AstNode, tries to speed it up
public class StructureMatcher
{
  // careful here, prevents garbage collection of AstNodes
  HashMap<AstNode,Integer> hashCache = new HashMap<AstNode,Integer>();
  public StructureMatcher() {}

  public int hashOf(AstNode node)
    { return node.getData().hashCode();  }

  public boolean checkEqual(AstNode a,AstNode b)
  {
    if(a==b) return true;
    if(hashOf(a)!=hashOf(b)) return false;
    if(a.getType()!=b.getType()) return false;
    AstNode[] ach = a.children(), bch = b.children();
    if(ach.length!=bch.length) return false;
    if(ach.length == 0) return a.getData().equals(b.getData());
    for(int i=0;i<ach.length;++i) if(!checkEqual(ach[i],bch[i])) return false;
    return true;
  }
  
}
