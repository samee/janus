package ast;

import java.util.HashMap;

public class StructureMap
{
  // warning, prevents garbage collection
  private HashMap<NodeBox,NodeBox> nodes = new HashMap<NodeBox,NodeBox>();
  private StructureMatcher smatcher = new StructureMatcher();

  /* if identical node was never seen before, returns null,
     and adds to a set. Else, nothing is added, and previously added
     node that is a duplicate of this is returned */
  public AstNode getDuplicate(AstNode node)
  {
    NodeBox nb = new NodeBox(node);
    NodeBox prev = nodes.get(nb);
    if(prev==null) nodes.put(nb,nb);
    return prev==null?null:prev.node;
  }

  private class NodeBox
  {
    AstNode node;
    NodeBox(AstNode n) { node = n; }
    public boolean equals(Object that)
    { if(this==that) return true;
      if(this.getClass()!=that.getClass()) return false;
      return smatcher.checkEqual(this.node,((NodeBox)that).node);
    }
    public int hashCode() { return smatcher.hashOf(node); }
  }
}
