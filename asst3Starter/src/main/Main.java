package main;

import java.io.*;

import parse.*;
import syntaxtree.ClassDecl;
import syntaxtree.Program;
import syntaxtree.AstNode;
import visitor.*;
import errorMsg.ErrorMsg;
import java.util.HashMap;

public class Main
{

    private static final boolean SCANNER_ONLY = false;

    private static final int SILENT = 0;
    private static final int PRINT = 1;
    private static final int PRETTY_PRINT = 2;
    private static final int VERBOSE_PRETTY_PRINT = 3;
    private static final int GEN_AST = 4;

    private int mode;

    private String inputFile;
    private String outputFile;
    private InputStream inputStream;
    private PrintStream outputStream;
    ErrorMsg errorMsg;

    public static void main(String args[])
    {
        Main mainObj = new Main();
        mainObj.parseCommandLine(args);
        mainObj.errorMsg = new ErrorMsg(mainObj.inputFile);
        mainObj.execMain();
    }

    void execMain()
    {
        try
        {
            openFiles();
            AstNode ast = (Program)parseFile();
            HashMap<String,ClassDecl> env = semanticPhase1(ast);
            semanticPhase2(ast, env);
            semanticPhase3(ast, env);
            printSummaryMessage();
            displayAst(ast);
            closeFiles();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            exitError("Unexpected exception: "+e);
        }
    }

    Main()
    {
        mode = SILENT;
        inputFile = null;
        outputFile = null;
    }

    void parseCommandLine(String[] args)
    {
        for(int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if(arg.length() == 0)      { exitError("Illegal argument: '"+arg+"'"); }
            else if(arg.equals("-g"))   { mode = GEN_AST; }
            else if(arg.equals("-p"))   { mode = PRINT; }
            else if(arg.equals("-pp"))  { mode = PRETTY_PRINT; }
            else if(arg.equals("-vpp")) { mode = VERBOSE_PRETTY_PRINT; }
            else if(arg.equals("-o"))
            {
                i++;
                if(outputFile != null)    { exitError("output file already defined"); }
                else if(i >= args.length) { exitError("-o requires a file"); }
                else                      { outputFile = args[i]; }
            }
            else if(arg.startsWith("-"))  { exitError("Invalid flag: " + arg); }
            else if(inputFile == null)    { inputFile = arg; }
            else                          { exitError("Only one input file allowed"); }
        }
        if(inputFile == null)             { exitError("No input file given"); }
    }

    private void openFiles()
    {
        try
        {
            inputStream = new FileInputStream(inputFile);
        }
        catch(FileNotFoundException e)
        {
            exitError("File not found: " + inputFile);
        }
        if(outputFile != null)
        {
            try
            {
                outputStream = new PrintStream(new FileOutputStream(outputFile));
            }
            catch(IOException e)
            {
                exitError("Error opening output file: " + outputFile);
            }
        }
        else
        {
            outputStream = System.out;
        }
    }

    private AstNode parseFile()
    {
        try
        {
            // get a big slab of bits
            // lex the input and write it to the slab,
            // then we'll feed that slab to the parser.
            // compress the errors we get in the scanner into a single error.
            ByteArrayOutputStream os = new ByteArrayOutputStream(1000000);
            ErrorMsg scanErr = new ErrorMsg(inputFile);
            new MJScannerParseTable(new MJScanner(scanErr, os)).parse(inputStream,0,false);

            if(scanErr.anyErrors)
            {
                errorMsg.error(-1, "Error detected during scanning");
            }
            os.close();
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

            // Make the parser and get the AST
            MJGrammar mj = new MJGrammar(errorMsg);
            new MJGrammarParseTable(mj).parse(is,0,false);
            is.close();
            return mj.parseResult();
        }
        catch(IOException iox)
        {
            System.err.println("Internal piping error.");
            return null;
        }
    }

    public HashMap<String,ClassDecl> semanticPhase1(AstNode ast)
    {
        if(!errorMsg.anyErrors && ast != null)
        {
            ClassListVisitor clv = new ClassListVisitor();
            ast.accept(clv);
            for(String cls : clv.classes())
            {
                System.out.println("class: " + cls);
            }
            
            Sem1Visitor vis = new Sem1Visitor(errorMsg);
            ast.accept(vis);
            return vis.getEnv();
        }
        else
        {
            return null;
        }
    }

    public void semanticPhase2(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if(!errorMsg.anyErrors && ast != null)
        {
            ast.accept(new Sem2Visitor(env, errorMsg));
        }
    }

    public void semanticPhase3(AstNode ast, HashMap<String,ClassDecl> env)
    {
        if(!errorMsg.anyErrors && ast != null)
        {
            ast.accept(new Sem3Visitor(env, errorMsg));
        }
    }

    public void closeFiles()
    {
        try
        {
            if(inputStream  != null) inputStream.close();
            if(outputStream != null) outputStream.close();
        }
        catch(IOException iox)
        {
            exitError("Error closing files.");
        }
    }

    public void printSummaryMessage()
    {
        errorMsg.printMessages();
        if(errorMsg.anyErrors)
        {
            System.err.println("Compilation failed for "+inputFile+".");
        }
        else
        {
            System.err.println("Compilation successful for "+inputFile+".");
        }
    }

    public void displayAst(AstNode ast)
    {
        if(ast == null) return;
        switch(mode)
        {
            case SILENT: break;
            case PRETTY_PRINT: 
                ast.accept(new PrettyPrintVisitor(true, false, outputStream));
                break;
            case VERBOSE_PRETTY_PRINT: 
                ast.accept(new PrettyPrintVisitor(true, false, true, outputStream));
                break;
            case PRINT: 
                ast.accept(new TreeDrawerVisitor(outputStream));
                break;
            case GEN_AST: 
                System.out.println(ast.accept(new ASTGenVisitor()));
                break;
        }
    }

    public static void exitError(String msg)
    {
        System.err.println(msg);
        exit(1);
    }
    public static void exit(int code)
    {
        System.exit(code);
    }
}
