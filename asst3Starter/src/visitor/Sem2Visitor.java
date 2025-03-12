package visitor;

import syntaxtree.*;
import java.util.*;
import errorMsg.*;

// the purpose of this class is to
// - link each ClassDecl to the ClassDecl for its superclass 
//    (via its 'superLink')
// - link each ClassDecl to each of its subclasses 
//    (via the 'subclasses' instance variable)
// - ensure that there are no cycles in the inheritance hierarchy
// - ensure that no class has 'String' or 'RunMain' as its superclass
public class Sem2Visitor extends Visitor
{

    HashMap<String,ClassDecl> classEnv;
    ErrorMsg errorMsg;

    public Sem2Visitor(HashMap<String,ClassDecl> env, ErrorMsg e)
    {
        errorMsg = e;
        classEnv = env;
    }

    public Object visit(ClassDecl c)
    {
        if(c.superName.equals("String") || c.superName.equals("RunMain")) {
            errorMsg.error(c.pos, new IllegalSuperclassError(c.superName));
            return null;
        }
        if(!classEnv.containsKey(c.superName)) {
            errorMsg.error(c.pos, new UndefinedSuperclassError(c.superName));
            return null;
        }
        c.superLink = classEnv.get(c.superName);
        for(ClassDecl d : classEnv.values()) {
            if(d.superName == c.name) {
                c.subclasses.add(d);
            }
        }
        int n = classEnv.size();
        int i = 0;
        while(c.superLink != null) {
            c = c.superLink;
            i++;
            if(i > n) {
                errorMsg.error(c.pos, new InheritanceCycleError(c.name));
                return null;
            }
        }
        return null;
    }
}
