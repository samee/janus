package ast;
public class HashUtil
{
  public static int combine(int x,int y)
  {
      // Copied from C++ boost library
      x^=y+0x9e3779b9+(x<<6)+(x>>2);
      return x;
  }
}
