package ast;

// Reference to a symbolic or concrete character from an AstNode
public class AstCharRef {
	private char ch; // '#' means symbolic
	private int id; // a string-specific symbol ID.
        private String c; // reference to the string object containing this
                          //   if it is symbolic

	// In practice, it is the index of the character in string
	public AstCharRef() {
		ch = '#';
		id = -1;
	}

	public AstCharRef(String s, int ind) {
		ch = s.charAt(ind);
		if (ch == '#')
                {
                        c = s;
			id = ind;
                }
	}

	public boolean isValid() {
		return ch != '#' || id != -1;
	}

	public boolean isSymbolic() {
		return isValid() && ch == '#';
	}

	public int getId() {
		if (!isSymbolic())
			throw new BadRequest("ID of non-symbolic AstCharRef requested");
		else
			return id;
	}

	public char getChar() {
		if (!isValid())
			throw new BadRequest("Reading uninitialized AstCharRef object");
		else
			return ch;
	}

        public String getStringRef() { return c; }

        public boolean equals(Object oother)
        {
          if(oother.getClass()!=this.getClass()) return false;
          AstCharRef other = (AstCharRef)oother;
          if(this.isSymbolic()!=other.isSymbolic()) return false;
          if(!this.isSymbolic()) return this.ch==other.ch;
          // Intentionally comparing references here
          return this.c==other.c && this.id==other.id;
        }

        public int hashCode()
        {
          if(isSymbolic())
            return System.identityHashCode(c)^(new Integer(this.id).hashCode());
          else
            return "Concrete".hashCode()^
              (Integer.MAX_VALUE & new Character(ch).hashCode());
        }


        // Same as AstCharRef.hashCode(), but this one has to have the same
        //   value on client and server side, and thus cannot depend on
        //   reference address. Can also be solved if someday garbled circuits
        //   could be made to evaluate in an order independent of the order
        //   in which they were connected.
        public int chHash()
        {
          if(isSymbolic()) 
            return "Symb".hashCode()^(new Integer(id).hashCode());
          else return hashCode();
        }
	public static class BadRequest extends Error {
		public BadRequest(String msg) {
			super(msg);
		}
	}
}
