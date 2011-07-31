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

    public int hashCode()
    {
      if(hashInCache) return hashcache;
      int rv="Add".hashCode();
      for(AstNode child:children)
        rv^=child.getData().hashCode();
	  hashInCache = true;
      return hashcache=rv;
    }
}
