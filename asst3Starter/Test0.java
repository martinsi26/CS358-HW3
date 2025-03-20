class Main { // class Error18 {
public    void main() {
	new Error18b().exec2();
    }
}
class Error18a {
    public int exec() {
	return 4;
    }
}
class Error18b extends Error18a {
    public int exec2() {
	return abc;
    }
}