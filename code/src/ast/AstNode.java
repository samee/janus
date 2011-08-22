package ast;

// Container to actual node
public class AstNode {
	private AstNodeData data;

	public AstNode() {
	}

	public AstNode(AstNodeData data) {
		this.data = data;
	}

	public void setData(AstNodeData data) {
		this.data = data;
	}

	public AstNodeData getData() {
		return data;
	}

	public Class getType() {
		return data.getClass();
	}

	public AstNode[] children() {
		return data.childNodes();
	}
        /*
           This is really screwed up. I can't really override hashCode or equals
           here, since AstVisitedMap needs it (it's a simple wrapper over
           WeakHashMap). So now I have all sorts of hash code wrappers sprinkled
           all over the code. 
             AstVisitedMap uses the java default, based on references
             AstCircuit uses nested class StableHashNode. Default equals,
               but hashcodes need to be the same between client and server.
               AstCircuit is no longer used in the main code, to be removed.
             MinMaxRedundancy uses inner class HashKey that is a very normal
               hash function except that it ignores constant children.
             StructureMatcher overrides both hashCode and equals in the expected
               way, and does recursive traversals as usual.
           Many of these caches the returned hash value for speedup
           */
}
