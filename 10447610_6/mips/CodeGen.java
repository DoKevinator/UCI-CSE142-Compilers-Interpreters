package mips;

import java.util.regex.Pattern;

import ast.*;
import types.*;

public class CodeGen implements ast.CommandVisitor {
    
    private StringBuffer errorBuffer = new StringBuffer();
    private TypeChecker tc;
    private Program program;
    private ActivationRecord currentFunction;

    private String function_return;

    public CodeGen(TypeChecker tc)
    {
        this.tc = tc;
        this.program = new Program();
    }
    
    public boolean hasError()
    {
        return errorBuffer.length() != 0;
    }
    
    public String errorReport()
    {
        return errorBuffer.toString();
    }

    private class CodeGenException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        public CodeGenException(String errorMessage) {
            super(errorMessage);
        }
    }
    
    public boolean generate(Command ast)
    {
        try {
            currentFunction = ActivationRecord.newGlobalFrame();
            ast.accept(this);
            return !hasError();
        } catch (CodeGenException e) {
            return false;
        }
    }
    
    public Program getProgram()
    {
        return program;
    }

    @Override
    public void visit(ExpressionList node) {
        for( Expression e : node ) {
        	e.accept(this);
        }
    }

    @Override
    public void visit(DeclarationList node) {
        for( Declaration d : node ) {
        	d.accept(this);
        }
    }

    @Override
    public void visit(StatementList node) {
        for( Statement s : node ) {
        	s.accept(this);
        }
    }

    @Override
    public void visit(AddressOf node) {
        //node.accept(this);

        currentFunction.getAddress(program, "$t0", node.symbol());
        program.pushInt("$t0");
    }

    @Override
    public void visit(LiteralBool node) {

        program.appendInstruction("addi $t0, $zero, " + node.value().ordinal());
        program.pushInt( "$t0" );
    }

    @Override
    public void visit(LiteralFloat node) {
        program.appendInstruction("li.s $f0, " + node.value().floatValue());
        program.pushFloat( "$f0" );
    }

    @Override
    public void visit(LiteralInt node) {
    	program.appendInstruction("addi $t0, $zero, " + node.value().intValue());
        program.pushInt( "$t0" );
    }

    @Override
    public void visit(VariableDeclaration node) {
        currentFunction.add(program, node);
    }

    @Override
    public void visit(ArrayDeclaration node) {
        currentFunction.add(program, node);
    }

    @Override
    public void visit(FunctionDefinition node) {
        currentFunction = new ActivationRecord(node, currentFunction);

        String begin_function;
        function_return = program.newLabel();

        if( node.function().name().equals("main") ) {
        	begin_function = "main";
        } else {
        	begin_function = "crxfunction." + node.function().name();
        }

        int starting_position = program.appendInstruction(begin_function + ":");
        node.body().accept(this);

        program.insertPrologue(starting_position+1, currentFunction.stackSize() );

        program.appendInstruction(function_return + ":");
        if( !node.function().type().equivalent( new VoidType() ) ) {
        	if( node.function().type().equivalent( new FloatType() ) ) {
	        	program.popFloat("$v0");
	        } else {
	        	program.popInt("$v0");
	        }
        }

        if( node.function().name().equals("main") ) {
        	program.appendExitSequence();	
        } else {
        	program.appendEpilogue( currentFunction.stackSize() );
        }
        
        currentFunction = currentFunction.parent();

    }

    @Override
    public void visit(Addition node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new FloatType() ) ) {
        	//right
        	program.popFloat("$f0");
        	//left
        	program.popFloat("$f2");

        	program.appendInstruction("add.s $f4, $f0, $f2");
        	program.pushFloat("$f4");
        } else {
        	program.popInt("$t0");
        	program.popInt("$t1");

        	program.appendInstruction("add $t2, $t0, $t1");
        	program.pushInt("$t2");
        }

    }

    @Override
    public void visit(Subtraction node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new FloatType() ) ) {
        	//right
        	program.popFloat("$f0");
        	//left
        	program.popFloat("$f2");

        	program.appendInstruction("sub.s $f4, $f2, $f0");
        	program.pushFloat("$f4");
        } else {
        	program.popInt("$t0");
        	program.popInt("$t1");

        	program.appendInstruction("sub $t2, $t1, $t0");
        	program.pushInt("$t2");
        }
    }

    @Override
    public void visit(Multiplication node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new FloatType() ) ) {
        	//right
        	program.popFloat("$f0");
        	//left
        	program.popFloat("$f2");

        	program.appendInstruction("mul.s $f4, $f0, $f2");
        	program.pushFloat("$f4");
        } else {
        	program.popInt("$t0");
        	program.popInt("$t1");

        	program.appendInstruction("mul $t2, $t0, $t1");
        	program.pushInt("$t2");
        }
    }

    @Override
    public void visit(Division node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new FloatType() ) ) {
        	//right
        	program.popFloat("$f0");
        	//left
        	program.popFloat("$f2");

        	program.appendInstruction("div.s $f4, $f2, $f0");
        	program.pushFloat("$f4");
        } else {
        	program.popInt("$t0");
        	program.popInt("$t1");

        	program.appendInstruction("div $t2, $t1, $t0");
        	program.pushInt("$t2");
        }
    }

    @Override
    public void visit(LogicalAnd node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new BoolType() ) ) {
        	program.popInt("$t0");
        	program.popInt("$t1");

        	program.appendInstruction("and $t2, $t0, $t1");
        	program.pushInt("$t2");
        }
    }

    @Override
    public void visit(LogicalOr node) {
        node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new BoolType() ) ) {
        	program.popFloat("$t0");
        	program.popFloat("$t1");

        	program.appendInstruction("or $t2, $t0, $t1");
        	program.pushFloat("$t2");
        }
    }
    
    @Override
    public void visit(LogicalNot node) {
        node.expression().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new BoolType() ) ) {
        	program.popInt("$t0");
        	program.appendInstruction("lui $t1, 0xFFFF");
        	program.appendInstruction("sra $t1, $t1, 16");
        	program.appendInstruction("xor $t2, $t1, $t0");
        	program.pushInt("$t2");
        }
    }

    @Override
    public void visit(Comparison node) {
    	node.leftSide().accept(this);
        node.rightSide().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new FloatType() ) ) {
        	//right
        	program.popFloat("$f0");
        	//left
        	program.popFloat("$f2");

        	program.appendInstruction( "c." + node.operation().toString().toLowerCase() + ".s $f2, $f0");
        } else {
        	program.popInt("$t0");
        	program.popInt("$t1");

        	program.appendInstruction( "s" + node.operation().toString().toLowerCase() + " $t2, $t1, $t0");
        	program.pushInt("$t2");
        }
    }

    @Override
    public void visit(Dereference node) {
    	node.expression().accept(this);

    	Type node_type = tc.getType(node);

    	//address of the local variable put into $t0
    	program.popInt("$t0");

    	if( node_type.equivalent( new IntType() ) ) {
    		program.appendInstruction("lw, $t1, 0($t0)");
    		program.pushInt("$t1");	
    	} else {
    		program.appendInstruction("lwc1, $f0, 0($t0)");
    		program.pushFloat("$f0");
    	}
    	
	}

    @Override
    public void visit(Index node) {
   		node.base().accept(this);
   		node.amount().accept(this);

   		program.popInt("$t0"); //amount
   		program.popInt("$t1"); //base

   		//base + (amount + offset)
   		Type typeOfNode;
   		if( tc.getType(node) instanceof AddressType ) {
   			typeOfNode = tc.getType(node).deref();
   		} else {
   			typeOfNode = tc.getType(node);
   		}

   		int number_of_bytes = currentFunction.numBytes( typeOfNode );
   		program.appendInstruction("addi $t2, $zero, " + number_of_bytes );
   		program.appendInstruction("mul $t0, $t0, $t2");
   		program.appendInstruction("add $t0, $t0, $t1");
   		program.pushInt("$t0");
    }

    @Override
    public void visit(Assignment node) {
        node.destination().accept(this);
        node.source().accept(this);

        Type node_type = tc.getType(node);

        if( node_type.equivalent( new FloatType() ) ) {
        	program.popFloat("$f2");	//source
        	program.popInt("$t0");	//dest (this is int because its the address)

        	program.appendInstruction("swc1 $f2, 0($t0)");
        } else {
        	program.popInt("$t1");	//source
        	program.popInt("$t0");	//dest

        	program.appendInstruction("sw $t1, 0($t0)");
        }
    }

    @Override
    public void visit(Call node) {
        node.arguments().accept(this);

        program.appendInstruction("jal crxfunction." + node.function().name());

        //caller teardown
        if( node.arguments().size() != 0 ) {
        	int argument_bytes = 0;
	        for( Expression e : node.arguments() ) {
	        	argument_bytes += currentFunction.numBytes(tc.getType( (Command) e));
	        }

	        program.appendInstruction("addi $sp, $sp, " + argument_bytes);
        }

        if( !node.function().type().equivalent( new VoidType() ) ) {
        	program.appendInstruction("subu $sp, $sp, 4");
        	program.appendInstruction("sw $v0, 0($sp)");
        }
    }

    @Override
    public void visit(IfElseBranch node) {
        node.condition().accept(this);

        String else_block = program.newLabel();
        String done_block = program.newLabel();

		//visit(node.condition());
        Type condition_type = tc.getType( (Command) node.condition());
        if( condition_type.equivalent( new FloatType() ) ) {	//float type comparisons
        	program.appendInstruction("bc1f " + else_block);
        } else {					//int or bool type comparisons
        	program.popInt("$t0");
        	program.appendInstruction("beq $t0, $zero, " + else_block);	
        }

		// then block instructions 
		node.thenBlock().accept(this);
		program.appendInstruction("j " + done_block);

		program.appendInstruction(else_block + ":");
		//else block instructions
		node.elseBlock().accept(this);

		program.appendInstruction(done_block + ":");
		//done with if else block
    }

    @Override
    public void visit(WhileLoop node) {
        
    	String loop = program.newLabel();
    	String end_loop = program.newLabel();

    	Type condition_type = tc.getType( (Command) node.condition());

    	program.appendInstruction(loop + ":");
    	node.condition().accept(this);
    	
    	program.popInt("$t0");
    	program.appendInstruction("beq $t0, $zero, " + end_loop);	
        
        node.body().accept(this);
        program.appendInstruction("j " + loop);

        program.appendInstruction(end_loop + ":");
    }

    @Override
    public void visit(Return node) {
        node.argument().accept(this);

        // Type node_type = tc.getType(node);

        program.appendInstruction("j " + function_return);
        // if( !node_type.equivalent( new VoidType() ) ) {
        // 	if( node_type.equivalent( new FloatType() ) ) {
	       //  	program.popFloat("$v0");
	       //  } else {
	       //  	program.popInt("$v0");
	       //  }
        // }
    }

    @Override
    public void visit(ast.Error node) {
        String message = "CodeGen cannot compile a " + node;
        errorBuffer.append(message);
        throw new CodeGenException(message);
    }
}
