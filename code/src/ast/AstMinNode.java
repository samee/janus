package ast;

public class AstMinNode implements AstNodeData {
	private AstNode[] children;
    private int hashcache=-1;
	private boolean hashInCache = false;
	private boolean depA = false, depB = false;

	public AstMinNode(AstNode[] children) {
		this.children = children;
		for(int i=0;i<children.length;++i)
		{	if(children[i].dependsOnA()) depA=true;
			if(children[i].dependsOnB()) depB=true;
		}
	}

	public AstNode[] childNodes() {
		return children;
	}

	// convenience constructor
	public static AstNode create(AstNode[] children) {
		return new AstNode(new AstMinNode(children));
	}

    public int hashCode()
    {
      if(hashInCache) return hashcache;
      int rv=HashUtil.combine("Min".hashCode(),
          ("Length"+children.length).hashCode());
      for(int i=0;i<children.length;++i)
        rv=HashUtil.combine(rv,children[i].getData().hashCode());
      hashInCache=true;
      return hashcache=rv;
    }

	public boolean dependsOnA() { return depA; }
	public boolean dependsOnB() { return depB; }
}
