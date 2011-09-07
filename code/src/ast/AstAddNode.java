package ast;

public class AstAddNode implements AstNodeData {
	private AstNode[] children;
    private int hashcache = -1;
	private boolean hashInCache = false;

	public AstAddNode(AstNode[] children) {
		this.children = children;
	}

	public AstNode[] childNodes() {
		return children;
	}

	// convenience constructor
	public static AstNode create(AstNode[] children) {
		return new AstNode(new AstAddNode(children));
	}

    private static int rotate(int x) { return (x<<1)+(x<0?1:0); }
    public int hashCode()
    {
      if(hashInCache) return hashcache;
      int rv=HashUtil.combine("Add".hashCode(),
          ("Length"+children.length).hashCode());
      for(int i=0;i<children.length;++i)
        rv=HashUtil.combine(rv,children[i].getData().hashCode());
	  hashInCache = true;
      return hashcache=rv;
    }
}
