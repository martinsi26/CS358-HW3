package visitor;

import syntaxtree.*;
import java.util.*;
import errorMsg.*;
// The purpose of this class is to:
// - link each variable reference to its corresponding VarDecl
//    (via its 'link' field)
//   - undefined variable names are reported
// - link each type reference to its corresponding ClassDecl
//   - undefined type names are reported
// - link each Break expression to its enclosing While or Case statement
//   - a break that is not inside any while loop or case is reported
// - report conflicting local variable names (including formal parameter names)
// - ensure that no instance variable has the name 'length'
public class Sem3Visitor extends Visitor
{
    // current class we're visiting
    ClassDecl currentClass;

    // environment for names of classes
    HashMap<String, ClassDecl> classEnv;

    // environment for names of variables
    HashMap<String, VarDecl> localEnv;

    // set of initialized variables
    HashSet<String> init;

    // set of unused classes
    HashSet<String> unusedClasses;

    // set of unused local variables
    HashMap<String, ClassDecl> unusedLocals;

    // stack of while/switch
    Stack<BreakTarget> breakTargetStack;

    //error message object
    ErrorMsg errorMsg;

    // constructor
    public Sem3Visitor(HashMap<String,ClassDecl> env, ErrorMsg e)
    {
        errorMsg         = e;
        currentClass     = null;
        classEnv         = env;
        localEnv         = new HashMap<String,VarDecl>();
        breakTargetStack = new Stack<BreakTarget>();
        init             = new HashSet<String>();
    }

    public Object visit(ClassDecl c)
    {
        currentClass = c;
        c.decls.accept(this);
        return null;
    }

    public Object visit(MethodDecl m)
    {
        localEnv.clear();
        for(String name : localEnv.keySet()) {
            init.remove(name);
        }
        m.formals.accept(this);
        m.stmts.accept(this);
        return null;
    }

    public Object visit(InstVarDecl i)
    {
        if(i.name.equals("length")) {
            errorMsg.error(i.pos, new IllegalLengthError());
            return null;
        }
        init.add(i.name);
        return null;
    }

    public Object visit(FormalDecl f)
    {
        if(localEnv.containsKey(f.name)) {
            errorMsg.error(f.pos, new DuplicateVariableError(f.name));
            return null;
        }
        localEnv.put(f.name, f);
        init.add(f.name);
        return null;
    }

    public Object visit(LocalVarDecl l)
    {
        if(localEnv.containsKey(l.name)) {
            errorMsg.error(l.pos, new DuplicateVariableError(l.name));
            return null;
        }
        localEnv.put(l.name, l);
        l.initExp.accept(this);
        init.add(l.name);
        return null;
    }

    public Object visit(IdentifierExp e)
    {
        if(!init.contains(e.name)) {
            errorMsg.error(e.pos, new UninitializedVariableError(e.name));
            return null;
        }
        e.link = localEnv.get(e.name);
        if(!localEnv.containsKey(e.name)) {
            e.link = currentClass.fieldEnv.get(e.name);
            if(!currentClass.fieldEnv.containsKey(e.name)) {
                ClassDecl c = currentClass;
                while(c.superLink != null) {
                    if(c.superLink.fieldEnv.containsKey(e.name)) {
                        e.link = c.superLink.fieldEnv.get(e.name);
                        break;
                    }
                    c = c.superLink;
                }
            }
        }
        if(e.link == null) {
            errorMsg.error(e.pos, new UndefinedVariableError(e.name));
            return null;
        }
        return null;
    }

    public Object visit(IdentifierType t)
    {
        if(classEnv.containsKey(t.name)) {
            t.link = classEnv.get(t.name);
        } else {
            errorMsg.error(t.pos, new UndefinedVariableError(t.name));
        }
        return null;
    }

    public Object visit(While w)
    {
        breakTargetStack.push(w);
        w.exp.accept(this);
        w.body.accept(this);
        breakTargetStack.pop();
        return null;
    }

    public Object visit(Switch s)
    {
        breakTargetStack.push(s);
        s.exp.accept(this);
        s.stmts.accept(this);
        breakTargetStack.pop();
        return null;
    }

    public Object visit(Break b)
    {
        if(breakTargetStack.empty())
        {
            errorMsg.error(b.pos, new BreakError());
            return null;
        }
        b.breakLink = breakTargetStack.peek();
        return null;
    }

    public Object visit(Label l)
    {
        l.enclosingSwitch = (Switch)breakTargetStack.peek();
        return null;
    }

}
