package ast.circuit;

import ast.AstNode;

public interface AstNodeWidth
{
  public int bitsFor(AstNode node);
  public int[] bitsForComponents(AstNode node);

  public static class Fixed implements AstNodeWidth
  {
    private int wd;
    public Fixed(int wd) { this.wd = wd; }
    public int bitsFor(AstNode node) 
      { return node.getType()!=ast.AstNequNode.class?wd:1; }  // hack!
    public int[] bitsForComponents(AstNode node)
    {
      int[] rv = new int[node.children().length-1];
      for(int i=0;i<rv.length;++i) rv[i]=wd;
      return rv;
    }
  }
}
