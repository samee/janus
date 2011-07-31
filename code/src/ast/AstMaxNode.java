package ast;

public class AstMaxNode implements AstNodeData {
	private AstNode[] children;
    private int hashcache=-1;
	private boolean hashInCache = false;

	public AstMaxNode(AstNode[] children) {
		this.children = children;
	}

	public AstNode[] childNodes() {
		return children;
	}

	// convenience constructor
	public static AstNode create(AstNode[] children) {
		return new AstNode(new AstMaxNode(children));
	}

        public int hashCode()
        {
          if(hashInCache) return hashcache;
          int rv="Max".hashCode();
          for(AstNode child:children)
            rv^=child.getData().hashCode();
		  hashInCache = true;
          return hashcache=rv;
        }
}
