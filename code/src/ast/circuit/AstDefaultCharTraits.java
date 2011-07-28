package ast.circuit;

public class AstDefaultCharTraits implements AstCharTraits
{
  int width;
  public AstDefaultCharTraits() { width = 2; }
  public AstDefaultCharTraits(int wd) { width = wd; }
  public int bitsPerChar() { return width; }
  public int encode(char ch) { return ch-'A'; }
}
