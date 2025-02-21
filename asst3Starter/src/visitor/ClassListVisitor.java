package visitor;

import syntaxtree.*;
import java.util.*;

public class ClassListVisitor extends Visitor
{
    List<String> _classes;
    HashMap<String, VarDecl> localEnv;
    String _currentClass;

    public ClassListVisitor()
    {
        _classes = new ArrayList<>();
        localEnv = new HashMap<>();
    }

    public List<String> classes()
    {
        return _classes;
    }
    
    public Object visit(ClassDecl c)
    {
        _classes.add(c.name);
        _currentClass = c.name;
        c.decls.accept(this);
        return null;
    }

    public Object visit(MethodDecl m)
    {
        _classes.add(_currentClass + "." + m.name + "()");
        m.formals.accept(this);
        m.stmts.accept(this);
        return null;
    }

    public Object visit(InstVarDecl f)
    {
        _classes.add(_currentClass + "." + f.name);
        //localEnv.put(f.name, f);
        f.type.accept(this);
        return null;
    }

    public Object visit(FormalDecl f)
    {
        System.out.println(f.name + " " + f.uniqueId);
        localEnv.put(f.name, f);
        f.type.accept(this);
        return null;
    }

    public Object visit(LocalVarDecl l)
    {
        System.out.println(l.name + " " + l.uniqueId);
        localEnv.put(l.name, l);
        l.type.accept(this);
        l.initExp.accept(this);
        return null;
    }

    public Object visit(IdentifierExp e)
    {
        VarDecl v = localEnv.get(e.name);
        System.out.println(e.name + " " + v.uniqueId);
        return null;
    }
}