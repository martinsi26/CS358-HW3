package syntaxtree;

import visitor.Visitor;

/**
 * a compound statement (block): a list of statements enclosed in curly braces
 */
public class Block extends Statement
{

    // instance variables filled in by constructor
    public StatementList stmts; // the list of the block's statements

    /**
     * constructor
     * @param pos file position
     * @param astmts the block's list of statements
     */
    public Block(int pos, StatementList astmts)
    {
        super(pos);
        stmts = astmts;
    }

    /*** remaining methods are visitor- and display-related ***/

    public Object accept(Visitor v)
    {
        return v.visit(this);
    }

}

