class A extends E{
    int x;
    public void foo() { }
}

class B extends A { }
class C extends B { }
class D extends C 
{
    int y;
    public int cow(int x) {
        int x = 0;
        return 0;
    }
 }
class E extends D { }
