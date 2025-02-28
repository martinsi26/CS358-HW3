package visitor;

import syntaxtree.*;
import java.util.*;
import errorMsg.*;
// The purpose of the Sem1Visitor class is to:
// - enter each class declaration into the global environment
//   - duplicate class names are detected
// - enter each method declaration into the method environment for its class
//   - duplicate method names for a class are detected
// - enter each field declaration into the field environment for its class
//   - duplicate instance variable names for a class are detected
// - all of the above are also done for the predefined classes
//   (Object, String, Lib and RunMain)
public class Sem1Visitor extends Visitor
{
    HashMap<String,ClassDecl> classEnv;
    ClassDecl currentClass;
    ClassDeclList classes;
    ErrorMsg errorMsg;

    public Sem1Visitor(ErrorMsg e)
    {
        errorMsg     = e;
        currentClass = null;
        classes      = new ClassDeclList();
        classEnv     = InitPredefined.initEnv(classes);
    }

    public HashMap<String,ClassDecl> getEnv()
    {
        return classEnv;
    }

    /**
     * Inserts the predefined class declarations into the Program
     * object so that they can be displayed, and so that the Sem1-,
     * Sem2-, and Sem3-visitors will traverse them.
     *
     * **** CS 358 Students should NOT modify this method ****
     */
    @Override
    public Object visit(Program p)
    {
        p.setDummyNodes(classes);
        p.classDecls.accept(this);
        return null;
    }

    public Object visit(ClassDecl c)
    {
        if(classEnv.containsKey(c.name)) {
            errorMsg.error(c.pos, new DuplicateClassError(c.name));
            return null;
        }
        classEnv.put(c.name, c);
        currentClass = c;
        c.decls.accept(this);
        return null;
    }

    public Object visit(MethodDecl m)
    {
        if(currentClass.methodEnv.containsKey(m.name)) {
            errorMsg.error(m.pos, new DuplicateMethodError(m.name));
            return null;
        }
        currentClass.methodEnv.put(m.name, m);
        m.formals.accept(this);
        m.stmts.accept(this);
        return null;
    }

    public Object visit(InstVarDecl f)
    {
        if(currentClass.fieldEnv.containsKey(f.name)) {
            errorMsg.error(f.pos, new DuplicateFieldError(f.name));
            return null;
        }
        currentClass.fieldEnv.put(f.name, f);
        return null;
    }
}
