/*
 * #%~
 * New Pretty Printer
 * %%
 * Copyright (C) 2008 - 2014 Overture
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #~%
 */
package org.overture.prettyprinter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.overture.ast.analysis.AnalysisException;
import org.overture.ast.analysis.QuestionAnswerAdaptor;
import org.overture.ast.definitions.AClassClassDefinition;
import org.overture.ast.definitions.AExplicitFunctionDefinition;
import org.overture.ast.definitions.AExplicitOperationDefinition;
import org.overture.ast.definitions.AInstanceVariableDefinition;
import org.overture.ast.definitions.ANamedTraceDefinition;
import org.overture.ast.definitions.AStateDefinition;
import org.overture.ast.definitions.ASystemClassDefinition;
import org.overture.ast.definitions.ATypeDefinition;
import org.overture.ast.definitions.AValueDefinition;
import org.overture.ast.definitions.PDefinition;
import org.overture.ast.definitions.SClassDefinition;
import org.overture.ast.definitions.SFunctionDefinition;
import org.overture.ast.definitions.SOperationDefinition;
import org.overture.ast.definitions.traces.ALetBeStBindingTraceDefinition;
import org.overture.ast.expressions.ABooleanConstExp;
import org.overture.ast.expressions.ACaseAlternative;
import org.overture.ast.expressions.ACasesExp;
import org.overture.ast.expressions.ADivNumericBinaryExp;
import org.overture.ast.expressions.ADivideNumericBinaryExp;
import org.overture.ast.expressions.AExistsExp;
import org.overture.ast.expressions.AForAllExp;
import org.overture.ast.expressions.AImpliesBooleanBinaryExp;
import org.overture.ast.expressions.AIntLiteralExp;
import org.overture.ast.expressions.ALambdaExp;
import org.overture.ast.expressions.ALetBeStExp;
import org.overture.ast.expressions.ALetDefExp;
import org.overture.ast.expressions.AMapCompMapExp;
import org.overture.ast.expressions.AModNumericBinaryExp;
import org.overture.ast.expressions.APlusNumericBinaryExp;
import org.overture.ast.expressions.ARealLiteralExp;
import org.overture.ast.expressions.ASetCompSetExp;
import org.overture.ast.expressions.ASubtractNumericBinaryExp;
import org.overture.ast.expressions.ATimesNumericBinaryExp;
import org.overture.ast.expressions.AVariableExp;
import org.overture.ast.expressions.PExp;
import org.overture.ast.intf.lex.ILexBooleanToken;
import org.overture.ast.intf.lex.ILexLocation;
import org.overture.ast.intf.lex.ILexNameToken;
import org.overture.ast.lex.LexLocation;
import org.overture.ast.lex.LexNameList;
import org.overture.ast.modules.AModuleModules;
import org.overture.ast.node.INode;
import org.overture.ast.patterns.ABooleanPattern;
import org.overture.ast.patterns.AIdentifierPattern;
import org.overture.ast.patterns.PMultipleBind;
import org.overture.ast.patterns.PPattern;
import org.overture.ast.statements.ABlockSimpleBlockStm;
import org.overture.ast.statements.ACallStm;
import org.overture.ast.statements.ACaseAlternativeStm;
import org.overture.ast.statements.ACasesStm;
import org.overture.ast.statements.AForAllStm;
import org.overture.ast.statements.AForIndexStm;
import org.overture.ast.statements.AForPatternBindStm;
import org.overture.ast.statements.AIdentifierStateDesignator;
import org.overture.ast.statements.ALetBeStStm;
import org.overture.ast.statements.ALetStm;
import org.overture.ast.statements.AReturnStm;
import org.overture.ast.statements.ATixeStm;
import org.overture.ast.statements.ATixeStmtAlternative;
import org.overture.ast.statements.ATrapStm;
import org.overture.ast.statements.PStm;
import org.overture.ast.typechecker.NameScope;
import org.overture.ast.types.ABooleanBasicType;
import org.overture.ast.types.AFieldField;
import org.overture.ast.types.ANamedInvariantType;
import org.overture.ast.types.AOperationType;
import org.overture.ast.types.ATokenBasicType;
import org.overture.ast.types.PType;
import org.overture.codegen.analysis.vdm.DefinitionInfo;
import org.overture.codegen.analysis.vdm.IdDesignatorOccurencesCollector;
import org.overture.codegen.analysis.vdm.IdOccurencesCollector;
import org.overture.codegen.analysis.vdm.NameCollector;
import org.overture.codegen.analysis.vdm.Renaming;
import org.overture.codegen.analysis.vdm.VarOccurencesCollector;
import org.overture.codegen.ir.TempVarNameGen;
import org.overture.core.npp.IPrettyPrinter;
import org.overture.core.npp.ISymbolTable;
import org.overture.core.npp.IndentTracker;
import org.overture.core.npp.Utilities;
import org.overture.typechecker.assistant.ITypeCheckerAssistantFactory;
import org.overture.typechecker.assistant.definition.SFunctionDefinitionAssistantTC;

class ASTPrettyPrinter extends QuestionAnswerAdaptor < IndentTracker, String >
    implements IPrettyPrinter {

	private ITypeCheckerAssistantFactory af;
	private static String space = " ";
	private PDefinition enclosingDef;
	private Map<AIdentifierStateDesignator, PDefinition> idDefs;
	private Stack<ILexNameToken> localDefsInScope;
	private int enclosingCounter;
	private Stack<String> stringStack;
	private Set<String> namesToAvoid;
	private TempVarNameGen nameGen;
	private boolean operationFlag;
	private boolean functionFlag;
	
	private int outerScopeCounter = -1;
	private List<Stack<String>> stringOuterStack;
	
	
	private Logger log = Logger.getLogger(this.getClass().getSimpleName());
	private String[] parameters;
	protected ISymbolTable mytable;
	protected IPrettyPrinter rootNpp;
	
	public ASTPrettyPrinter(IPrettyPrinter root, ISymbolTable nst, ITypeCheckerAssistantFactory af,
			Map<AIdentifierStateDesignator, PDefinition> idDefs)
	{
		this.rootNpp = root;
		this.mytable = nst;
		this.af = af;
		this.enclosingDef = null;
		this.idDefs = idDefs;
		this.localDefsInScope = new Stack<ILexNameToken>();
		this.enclosingCounter = 0;
		this.stringStack = new Stack<String>();
		this.namesToAvoid = new HashSet<String>();
		this.nameGen = new TempVarNameGen();
		this.stringOuterStack = new ArrayList<Stack<String>>();
	}

	@Override
	public String caseAModuleModules(AModuleModules node, IndentTracker question) throws AnalysisException
	{
		if (enclosingDef != null)
		{
			return null;
		}
		
		incrementOuterScopeCounter();
		if(!node.getName().getName().equals("DEFAULT")){
			insertIntoStringStack(node.getName().getName() + "\n");
		}

		visitModuleDefs(node.getDefs(), node, question);
		
		if(!node.getName().getName().equals("DEFAULT")){
			insertIntoStringStack("end " + node.getName().getName() + ";\n");
		}
		return node.getName().getName();
	}

	@Override
	public String caseAClassClassDefinition(AClassClassDefinition node, IndentTracker question)
			throws AnalysisException
	{
		if (enclosingDef != null)
		{
			return null;
		}
		insertIntoStringStack(node.getName().getFullName());
		visitModuleDefs(node.getDefinitions(), node, question);
		return node.getName().getFullName();
	}

//	@Override
//	public String caseASystemClassDefinition(ASystemClassDefinition node)
//			throws AnalysisException
//	{
//		if (enclosingDef != null)
//		{
//			return;
//		}
//
//		visitModuleDefs(node.getDefinitions(), node);
//	}

	// For operations and functions it works as a single pattern
	// Thus f(1,mk_(2,2),5) will fail
	// public f : nat * (nat * nat) * nat -> nat
	// f (b,mk_(b,b), a) == b;

	@Override
	public String caseAExplicitOperationDefinition(
			AExplicitOperationDefinition node, IndentTracker question) throws AnalysisException
	{
		
		if (!proceed(node))
		{
			return null;
		}
		if(!operationFlag){
			insertIntoStringStack("\noperations\n\n");
		}
		
		StringBuilder strBuilder = new StringBuilder();
		String opName = node.getName().getFullName();
		strBuilder.append(opName);
		strBuilder.append(" : ");
		
		VDMDefinitionInfo defInfo = new VDMDefinitionInfo(node.getParamDefinitions(), af);
		
		
		for (PDefinition def : defInfo.getNodeDefs()){
			if(def != defInfo.getNodeDefs().get(0)){
				strBuilder.append(", ");
			}	
			strBuilder.append(def.getType().toString());
		}
		strBuilder.append(" ==> ");
		AOperationType defOp = node.getType().getAncestor(AOperationType.class);
		if (defOp != null){
			strBuilder.append(defOp.getResult().toString());
		}
		
		// Top part done:" op: nat ==> nat "
		strBuilder.append("\n");
		strBuilder.append(opName + "(");
		
		for (PDefinition def : defInfo.getNodeDefs()){
			if(def != defInfo.getNodeDefs().get(0)){
				strBuilder.append(", ");
			}	
			strBuilder.append(def.getName());
		}
		strBuilder.append(") ==");
		strBuilder.append(" \n");
		insertIntoStringStack(strBuilder.toString());

		node.getBody().apply(this, question);

		endScope(defInfo);

		strBuilder = new StringBuilder();
		strBuilder.append(";");
		insertIntoStringStack(strBuilder.toString());
		return node.getName().getFullName();
	}
//
//	@Override
//	public String caseAExplicitFunctionDefinition(
//			AExplicitFunctionDefinition node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		DefinitionInfo defInfo = new DefinitionInfo(getParamDefs(node), af);
//
//		openScope(defInfo, node);
//
//		node.getBody().apply(this);
//
//		endScope(defInfo);
//	}
//
//	@Override
//	public String caseABlockSimpleBlockStm(ABlockSimpleBlockStm node)
//			throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		DefinitionInfo defInfo = new DefinitionInfo(node.getAssignmentDefs(), af);
//
//		visitDefs(defInfo.getNodeDefs());
//
//		openScope(defInfo, node);
//
//		visitStms(node.getStatements());
//
//		endScope(defInfo);
//	}
//
//	@Override
//	public String caseALetDefExp(ALetDefExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		DefinitionInfo defInfo = new DefinitionInfo(node.getLocalDefs(), af);
//
//		visitDefs(defInfo.getNodeDefs());
//
//		openScope(defInfo, node);
//
//		node.getExpression().apply(this);
//
//		endScope(defInfo);
//	}
//
	@Override
	public String caseALetStm(ALetStm node, IndentTracker question) throws AnalysisException
	{
		if (!proceed(node))
		{
			return null;
		}
		StringBuilder strBuilder = new StringBuilder();
		
		strBuilder.append("let ");
		
		VDMDefinitionInfo defInfo = new VDMDefinitionInfo(node.getLocalDefs(), af);

		visitDefs(defInfo.getNodeDefs(), question);

//		openScope(defInfo, node, question);

		List<? extends PDefinition> nodeDefs = defInfo.getNodeDefs();

		for (PDefinition parentDef : nodeDefs)
		{
			if(parentDef != nodeDefs.get(0)){
				strBuilder.append(", ");
			}
			List<? extends PDefinition> localDefs = defInfo.getLocalDefs(parentDef);

			for (PDefinition localDef : localDefs){
				strBuilder.append(localDef.getName().getFullName() + " = ");
			} // check if it matches position
			insertIntoStringStack(strBuilder.toString());
			AValueDefinition defVal = parentDef.getAncestor(AValueDefinition.class);
			
			if (defVal != null){
				defVal.getExpression().apply(this, question);
			}
			
			strBuilder = new StringBuilder();
		}
		insertIntoStringStack("\nin\n");
		
		node.getStatement().apply(this, question);

		endScope(defInfo);
		return node.toString();
	}
	
	@Override
	public String caseAReturnStm(AReturnStm node, IndentTracker question) throws AnalysisException {
		insertIntoStringStack("return ");
		node.getExpression().apply(this, question);
		return node.toString();
	}
//
//	@Override
//	public String caseALetBeStExp(ALetBeStExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		node.getDef().apply(this);
//
//		DefinitionInfo defInfo = new DefinitionInfo(node.getDef().getDefs(), af);
//
//		openScope(defInfo, node);
//
//		if (node.getSuchThat() != null)
//		{
//			node.getSuchThat().apply(this);
//		}
//
//		node.getValue().apply(this);
//
//		endScope(defInfo);
//	}
//
//	/*
//	 * Exists1 needs no treatment it uses only a bind
//	 */
//
//	@Override
//	public String caseAForAllExp(AForAllExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleMultipleBindConstruct(node, node.getBindList(), null, node.getPredicate());
//	}
//
//	@Override
//	public String caseAExistsExp(AExistsExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleMultipleBindConstruct(node, node.getBindList(), null, node.getPredicate());
//	}
//
//	/*
//	 * Sequence comp needs no treatment it uses only a bind
//	 */
//
//	@Override
//	public String caseASetCompSetExp(ASetCompSetExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleMultipleBindConstruct(node, node.getBindings(), node.getFirst(), node.getPredicate());
//	}
//
//	@Override
//	public void caseAMapCompMapExp(AMapCompMapExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleMultipleBindConstruct(node, node.getBindings(), node.getFirst(), node.getPredicate());
//	}
//
//	@Override
//	public void caseALetBeStStm(ALetBeStStm node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		node.getDef().apply(this);
//
//		DefinitionInfo defInfo = new DefinitionInfo(node.getDef().getDefs(), af);
//
//		openScope(defInfo, node);
//
//		if (node.getSuchThat() != null)
//		{
//			node.getSuchThat().apply(this);
//		}
//
//		node.getStatement().apply(this);
//
//		endScope(defInfo);
//	}
//
//	@Override
//	public void caseALetBeStBindingTraceDefinition(
//			ALetBeStBindingTraceDefinition node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		node.getDef().apply(this);
//
//		DefinitionInfo defInfo = new DefinitionInfo(node.getDef().getDefs(), af);
//
//		openScope(defInfo, node);
//
//		if (node.getStexp() != null)
//		{
//			node.getStexp().apply(this);
//		}
//
//		node.getBody().apply(this);
//
//		endScope(defInfo);
//	}
//
//	@Override
//	public void caseALambdaExp(ALambdaExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		DefinitionInfo defInfo = new DefinitionInfo(node.getParamDefinitions(), af);
//
//		openScope(defInfo, node);
//
//		node.getExpression().apply(this);
//
//		endScope(defInfo);
//	}
//
//	@Override
//	public void caseATixeStm(ATixeStm node) throws AnalysisException
//	{
//		if (node.getBody() != null)
//		{
//			node.getBody().apply(this);
//		}
//
//		// The trap alternatives will be responsible for opening/ending the scope
//		for (ATixeStmtAlternative trap : node.getTraps())
//		{
//			trap.apply(this);
//		}
//	}
//
//	@Override
//	public void caseATixeStmtAlternative(ATixeStmtAlternative node)
//			throws AnalysisException
//	{
//		openScope(node.getPatternBind(), node.getPatternBind().getDefs(), node.getStatement());
//
//		node.getStatement().apply(this);
//
//		// End scope
//		for (PDefinition def : node.getPatternBind().getDefs())
//		{
//			removeLocalDefFromScope(def);
//		}
//	}
//
//	@Override
//	public void caseATrapStm(ATrapStm node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		if (node.getBody() != null)
//		{
//			node.getBody().apply(this);
//		}
//
//		openScope(node.getPatternBind().getPattern(), node.getPatternBind().getDefs(), node.getWith());
//
//		if (node.getWith() != null)
//		{
//			node.getWith().apply(this);
//		}
//
//		for (PDefinition def : node.getPatternBind().getDefs())
//		{
//			removeLocalDefFromScope(def);
//		}
//	}
//
//	@Override
//	public void caseAForPatternBindStm(AForPatternBindStm node)
//			throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		if (node.getExp() != null)
//		{
//			node.getExp().apply(this);
//		}
//
//		openScope(node.getPatternBind().getPattern(), node.getPatternBind().getDefs(), node.getStatement());
//
//		node.getStatement().apply(this);
//
//		for (PDefinition def : node.getPatternBind().getDefs())
//		{
//			removeLocalDefFromScope(def);
//		}
//	}
//
//	@Override
//	public void caseAForAllStm(AForAllStm node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		if (node.getSet() != null)
//		{
//			node.getSet().apply(this);
//		}
//
//		PType possibleType = af.createPPatternAssistant().getPossibleType(node.getPattern());
//		List<PDefinition> defs = af.createPPatternAssistant().getDefinitions(node.getPattern(), possibleType, NameScope.LOCAL);
//
//		for (PDefinition d : defs)
//		{
//			openLoop(d.getName(), node.getPattern(), node.getStatement());
//		}
//
//		node.getStatement().apply(this);
//
//		for (PDefinition def : defs)
//		{
//			removeLocalDefFromScope(def);
//		}
//	}
//
//	@Override
//	public void caseAForIndexStm(AForIndexStm node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		if (node.getFrom() != null)
//		{
//			node.getFrom().apply(this);
//		}
//
//		if (node.getTo() != null)
//		{
//			node.getTo().apply(this);
//		}
//
//		if (node.getBy() != null)
//		{
//			node.getBy().apply(this);
//		}
//
//		ILexNameToken var = node.getVar();
//
//		openLoop(var, null, node.getStatement());
//
//		node.getStatement().apply(this);
//
//		localDefsInScope.remove(var);
//	}
//
//	@Override
//	public void caseACasesStm(ACasesStm node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleCaseNode(node.getExp(), node.getCases(), node.getOthers());
//	}
//
//	@Override
//	public void caseACasesExp(ACasesExp node) throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleCaseNode(node.getExpression(), node.getCases(), node.getOthers());
//	}
//
//	@Override
//	public void caseACaseAlternativeStm(ACaseAlternativeStm node)
//			throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleCase(node.getDefs(), node.getPattern(), node.getResult());
//	}
//
//	@Override
//	public void caseACaseAlternative(ACaseAlternative node)
//			throws AnalysisException
//	{
//		if (!proceed(node))
//		{
//			return;
//		}
//
//		handleCase(node.getDefs(), node.getPattern(), node.getResult());
//	}
//
//	@Override
//	public void caseILexNameToken(ILexNameToken node) throws AnalysisException
//	{
//		// No need to visit names
//	}

	@Override
	public String caseAPlusNumericBinaryExp(APlusNumericBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS, question);
		String op = mytable.getPLUS();

		StringBuilder sb = new StringBuilder();

		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseASubtractNumericBinaryExp(ASubtractNumericBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS, question);
		String op = mytable.getMINUS();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseATimesNumericBinaryExp(ATimesNumericBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS,question);
		String op = mytable.getTIMES();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseADivideNumericBinaryExp(ADivideNumericBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS,question);
		String op = mytable.getDIVIDE();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseAModNumericBinaryExp(AModNumericBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS,question);
		String op = mytable.getMOD();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseADivNumericBinaryExp(ADivNumericBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS,question);
		String op = mytable.getDIV();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseAImpliesBooleanBinaryExp(AImpliesBooleanBinaryExp node,
			IndentTracker question) throws AnalysisException
	{
		String l = node.getLeft().apply(THIS, question);
		String r = node.getRight().apply(THIS,question);
		String op = mytable.getIMPLIES();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(l);
		sb.append(space);
		sb.append(op);
		sb.append(space);
		sb.append(r);
		insertIntoStringStack(Utilities.wrap(sb.toString()));
		return	Utilities.wrap(sb.toString());
	}
	
	@Override
	public String caseAIntLiteralExp(AIntLiteralExp node, IndentTracker question)
			throws AnalysisException
	{
		insertIntoStringStack(Long.toString(node.getValue().getValue()));
		return Long.toString(node.getValue().getValue());
	}
	
	@Override
	public String caseARealLiteralExp(ARealLiteralExp node,
			IndentTracker question) throws AnalysisException
	{
		insertIntoStringStack(Double.toString(node.getValue().getValue()));
		return Double.toString(node.getValue().getValue());
	}
	
	@Override
	public String caseAVariableExp(AVariableExp node, IndentTracker question)
			throws AnalysisException
	{
		String var = node.getOriginal();
		insertIntoStringStack(var);
		return var;
		
	}
	
	private void handleCaseNode(PExp cond, List<? extends INode> cases,
			INode others, IndentTracker question) throws AnalysisException
	{
		if (cond != null)
		{
			cond.apply(this, question);
		}

		// The cases will be responsible for opening/ending the scope
		for (INode c : cases)
		{
			c.apply(this, question);
		}

		if (others != null)
		{
			others.apply(this, question);
		}
	}

//	private void openLoop(ILexNameToken var, INode varParent, PStm body)
//			throws AnalysisException
//	{
//		if (!contains(var))
//		{
//			localDefsInScope.add(var);
//		} else
//		{
//			String newName = computeNewName(var.getName());
//
//			registerRenaming(var, newName);
//
//			if (varParent != null)
//			{
//				Set<AIdentifierPattern> idPatterns = collectIdOccurences(var, varParent);
//
//				for (AIdentifierPattern id : idPatterns)
//				{
//					registerRenaming(id.getName(), newName);
//				}
//			}
//
//			Set<AVariableExp> vars = collectVarOccurences(var.getLocation(), body);
//
//			for (AVariableExp varExp : vars)
//			{
//				registerRenaming(varExp.getName(), newName);
//			}
//
//			Set<AIdentifierStateDesignator> idStateDesignators = collectIdDesignatorOccurrences(var.getLocation(), body);
//
//			for (AIdentifierStateDesignator id : idStateDesignators)
//			{
//				registerRenaming(id.getName(), newName);
//			}
//		}
//	}
	@Override
	public String caseATokenBasicType(ATokenBasicType node, IndentTracker question) throws AnalysisException {
		// TODO Auto-generated method stub
		return super.caseATokenBasicType(node, question);
	}
	private void handleCase(LinkedList<PDefinition> localDefs, PPattern pattern,
			INode result, IndentTracker question) throws AnalysisException
	{
		// Do not visit the conditional exp (cexp)
		openScope(pattern, localDefs, result);

		result.apply(this, question);

		// End scope
		for (PDefinition def : localDefs)
		{
			removeLocalDefFromScope(def);
		}
	}

	private void handleMultipleBindConstruct(INode node,
			LinkedList<PMultipleBind> bindings, PExp first, PExp pred, IndentTracker question)
			throws AnalysisException
	{

		VDMDefinitionInfo defInfo = new VDMDefinitionInfo(getMultipleBindDefs(bindings), af);

		openScope(defInfo, node, question);

		if (first != null)
		{
			first.apply(this, question);
		}

		if (pred != null)
		{
			pred.apply(this, question);
		}

		endScope(defInfo);
	}

	private List<PDefinition> getMultipleBindDefs(List<PMultipleBind> bindings)
	{
		List<PDefinition> defs = new Vector<PDefinition>();

		for (PMultipleBind mb : bindings)
		{
			for (PPattern pattern : mb.getPlist())
			{
				defs.addAll(af.createPPatternAssistant().getDefinitions(pattern, af.createPMultipleBindAssistant().getPossibleType(mb), NameScope.LOCAL));
			}
		}

		return defs;
	}

	private void visitModuleDefs(List<PDefinition> defs, INode module, IndentTracker question)
			throws AnalysisException
	{
		VDMDefinitionInfo defInfo = getStateDefs(defs, module);

		if (defInfo != null)
		{
			addLocalDefs(defInfo);
			
			if(!defInfo.getAllLocalDefs().isEmpty()){
				insertIntoStringStack("\ntypes\n\n");
			}
			for (ATypeDefinition typeDef : defInfo.getTypeDefs()) // check if it matches position
			{		
				insertIntoStringStack(typeDef.getName().getFullName() + " = " + getTypeDefAncestor(typeDef) + ";\n");
			}
			
			if(!defInfo.getAllLocalDefs().isEmpty()){
				insertIntoStringStack("\nvalues\n\n");
			}
			for (PDefinition localDef : defInfo.getAllLocalDefs()) // check if it matches position
			{
				insertIntoStringStack(localDef.parent().toString() + ";\n");
			}
			handleExecutables(defs,question);
			removeLocalDefs(defInfo);
			
		} else
		{
			handleExecutables(defs,question);
		}
	}

	private String getTypeDefAncestor(ATypeDefinition node){
		
		ANamedInvariantType defInvType = node.getType().getAncestor(ANamedInvariantType.class);
		if (defInvType != null){
			return defInvType.getType().toString();
		}else{
			//ANamedInvariantType def = node.getType().getAncestor(ANamedInvariantType.class);
		}
			
		return  "";
	}
	
	private void handleExecutables(List<PDefinition> defs, IndentTracker question)
			throws AnalysisException
	{
		for (PDefinition def : defs)
		{
			if (def instanceof SOperationDefinition
					|| def instanceof SFunctionDefinition
					|| def instanceof ANamedTraceDefinition)
			{
				enclosingDef = def;
				enclosingCounter = 0;
				setNamesToAvoid(def);
				this.nameGen = new TempVarNameGen();

				def.apply(this,question);
			}
		}
	}

	private VDMDefinitionInfo getStateDefs(List<PDefinition> defs, INode module)
	{
		if (module instanceof AModuleModules)
		{
			List<PDefinition> fieldDefs = new LinkedList<PDefinition>();
			List<ATypeDefinition> typeDefs = new LinkedList<ATypeDefinition>();
			AStateDefinition stateDef = getStateDef(defs);
		
			if (stateDef != null)
			{
				fieldDefs.addAll(findFieldDefs(stateDef.getStateDefs(), stateDef.getFields()));
			}
			
			for (PDefinition def : defs)
			{
				if (def instanceof ATypeDefinition)
				{
					typeDefs.add((ATypeDefinition) def);
				}
			}
			
			for (PDefinition def : defs)
			{
				if (def instanceof AValueDefinition)
				{
					fieldDefs.add(def);
				}
			}

			return new VDMDefinitionInfo(fieldDefs, typeDefs, af);
		} else if (module instanceof SClassDefinition)
		{
			SClassDefinition classDef = (SClassDefinition) module;
			List<PDefinition> allDefs = new LinkedList<PDefinition>();

			LinkedList<PDefinition> enclosedDefs = classDef.getDefinitions();
			LinkedList<PDefinition> inheritedDefs = classDef.getAllInheritedDefinitions();

			allDefs.addAll(enclosedDefs);
			allDefs.addAll(inheritedDefs);

			List<PDefinition> fields = new LinkedList<PDefinition>();

			for (PDefinition def : allDefs)
			{
				if (def instanceof AInstanceVariableDefinition
						|| def instanceof AValueDefinition)
				{
					fields.add(def);
				}
			}

			return new VDMDefinitionInfo(fields, af);
		} else
		{
			log.error("Expected module or class definition. Got: " + module);
			return null;
		}
	}

	private AStateDefinition getStateDef(List<PDefinition> defs)
	{
		for (PDefinition def : defs)
		{
			if (def instanceof AStateDefinition)
			{
				return (AStateDefinition) def;
			}
		}

		return null;
	}
	
	
	private List<PDefinition> findFieldDefs(List<PDefinition> stateDefs,
			List<AFieldField> fields)
	{
		List<PDefinition> fieldDefs = new LinkedList<PDefinition>();

		for (PDefinition d : stateDefs)
		{
			for (AFieldField f : fields)
			{
				if (f.getTagname().equals(d.getName()))
				{
					fieldDefs.add(d);
					break;
				}
			}
		}
		return fieldDefs;
	}

	private void setNamesToAvoid(PDefinition def) throws AnalysisException
	{
		NameCollector collector = new NameCollector();
		def.apply(collector);
		namesToAvoid = collector.namesToAvoid();
	}

	public void init(boolean clearRenamings)
	{
		this.enclosingDef = null;
		this.enclosingCounter = 0;
		this.namesToAvoid.clear();
		this.nameGen = new TempVarNameGen();

//		if (renamings != null && clearRenamings)
//		{
//			renamings.clear();
//		}
	}

//	public Set<Renaming> getRenamings()
//	{
//		return renamings;
//	}

	private List<PDefinition> getParamDefs(AExplicitFunctionDefinition node)
	{
		SFunctionDefinitionAssistantTC funcAssistant = this.af.createSFunctionDefinitionAssistant();
		List<List<PDefinition>> paramDefs = funcAssistant.getParamDefinitions(node, node.getType(), node.getParamPatternList(), node.getLocation());

		List<PDefinition> paramDefFlattened = new LinkedList<PDefinition>();

		for (List<PDefinition> list : paramDefs)
		{
			paramDefFlattened.addAll(list);
		}

		return paramDefFlattened;
	}

	private boolean proceed(INode node)
	{
		if (node == enclosingDef)
		{
			enclosingCounter++;
		}

		if (enclosingCounter > 1)
		{
			// To protect against recursion
			return false;
		}

		PDefinition def = node.getAncestor(SOperationDefinition.class);

		if (def == null)
		{
			def = node.getAncestor(SFunctionDefinition.class);

			if (def == null)
			{
				def = node.getAncestor(ANamedTraceDefinition.class);

				if (def == null)
				{
					def = node.getAncestor(AValueDefinition.class);

					if (def == null)
					{
						def = node.getAncestor(AInstanceVariableDefinition.class);

						if (def == null)
						{
							def = node.getAncestor(ATypeDefinition.class);

							if (def == null)
							{
								def = node.getAncestor(AStateDefinition.class);
							}
						}
					}
				}
			}
		}

		if (def == null)
		{
			log.error("Got unexpected definition: " + enclosingDef);
		}

		return enclosingDef == def;
	}

	private void addLocalDefs(VDMDefinitionInfo defInfo)
	{

		List<PDefinition> allLocalDefs = defInfo.getAllLocalDefs();
		for (PDefinition localDef : allLocalDefs)
		{
			if (!contains(localDef))
			{
				localDefsInScope.add(localDef.getName());
			}
		}
	}

	private void removeLocalDefs(VDMDefinitionInfo defInfo)
	{
		localDefsInScope.removeAll(defInfo.getAllLocalDefNames());
	}

	public void openScope(VDMDefinitionInfo defInfo, INode defScope, IndentTracker question)
			throws AnalysisException
	{
		List<? extends PDefinition> nodeDefs = defInfo.getNodeDefs();

		for (int i = 0; i < nodeDefs.size(); i++)
		{
			PDefinition parentDef = nodeDefs.get(i);

			List<? extends PDefinition> localDefs = defInfo.getLocalDefs(parentDef);

			for (PDefinition localDef : localDefs) // check if it matches position
			{
				insertIntoStringStack(localDef.toString());
			}
		}
	}

	public void openScope(INode parentNode, List<PDefinition> localDefs,
			INode defScope) throws AnalysisException
	{
		for (PDefinition localDef : localDefs)
		{
//			if(CompareNodeLocation(localDef.getLocation()) || checkVarOccurences(localDef.getLocation(), defScope)){
//				findRenamings(localDef, defScope.parent(), defScope);
//			}
		}
	}

	public void endScope(VDMDefinitionInfo defInfo)
	{
		this.localDefsInScope.removeAll(defInfo.getAllLocalDefNames());
	}

	public void removeLocalDefFromScope(PDefinition localDef)
	{
		localDefsInScope.remove(localDef.getName());
	}

	


	private Set<ACallStm> collectCallOccurences(ILexLocation defLoc, INode defScope) throws AnalysisException {
		CallOccurenceCollector collector = new CallOccurenceCollector(defLoc);
		defScope.apply(collector);
		return collector.getCalls();
	}



	private Set<AVariableExp> collectVarOccurences(ILexLocation defLoc,
			INode defScope) throws AnalysisException
	{
		VarOccurencesCollector collector = new VarOccurencesCollector(defLoc);

		defScope.apply(collector);

		return collector.getVars();
	}

	private boolean checkVarOccurences(ILexLocation defLoc,
			INode defScope) throws AnalysisException
	{
		VarOccurencesCollector collector = new VarOccurencesCollector(defLoc);
		
		defScope.apply(collector);
		Set<AVariableExp> setOfVars = collector.getVars();
		
		for(Iterator<AVariableExp> i = setOfVars.iterator(); i.hasNext(); ) {
			AVariableExp item = i.next();
			if(CompareNodeLocation(item.getLocation())){
				return true;
			}
		}
		
		return false;
	}
	
	private Set<AIdentifierStateDesignator> collectIdDesignatorOccurrences(
			ILexLocation defLoc, INode defScope) throws AnalysisException
	{
		IdDesignatorOccurencesCollector collector = new IdDesignatorOccurencesCollector(defLoc, idDefs);

		defScope.apply(collector);

		return collector.getIds();
	}

	private Set<AIdentifierPattern> collectIdOccurences(ILexNameToken name,
			INode parent) throws AnalysisException
	{
		IdOccurencesCollector collector = new IdOccurencesCollector(name, parent);

		parent.apply(collector);

		return collector.getIdOccurences();
	}

	private boolean contains(PDefinition defToCheck)
	{
		ILexNameToken nameToCheck = getName(defToCheck);

		return contains(nameToCheck);
	}

	private boolean contains(ILexNameToken nameToCheck)
	{
		// Can be null if we try to find the name for the ignore pattern
		if (nameToCheck != null)
		{
			for (ILexNameToken name : localDefsInScope)
			{
				if (name != null
						&& nameToCheck.getName().equals(name.getName()))
				{
					return true;
				}
			}
		}

		return false;
	}

	private ILexNameToken getName(PDefinition def)
	{
		LexNameList varNames = af.createPDefinitionAssistant().getVariableNames(def);

		if (varNames.isEmpty())
		{
			// Can be empty if we try to find the name for the ignore pattern
			return null;
		} else
		{
			return varNames.firstElement();
		}
	}

	private void visitDefs(List<? extends PDefinition> defs, IndentTracker question)
			throws AnalysisException
	{
		for (PDefinition def : defs)
		{
			def.apply(this, question);
		}
	}

	private void visitStms(List<? extends PStm> stms, IndentTracker question) throws AnalysisException
	{
		for (PStm stm : stms)
		{
			stm.apply(this, question);
		}
	}
	
	private boolean CompareNodeLocation(ILexLocation newNode){

		System.out.println("Pos " + newNode.getEndLine() + ": " + newNode.getEndPos());

		if(parameters.length >= 4){
			if(newNode.getEndLine() == Integer.parseInt(parameters[0]) &&
//					newNode.getEndOffset() == Integer.parseInt(parameters[1]) && //TODO Check if it is needed
							newNode.getEndPos() == Integer.parseInt(parameters[2])){
				return true;
			}
		}
		return false;
	}
	
	public String getVDMText(){
		StringBuilder strBuilder = new StringBuilder();
		for(Stack<String> list : stringOuterStack){
			
		    for(String item : list)
			{
				if(item != list.get(0)){
					//strBuilder.append("\n");
				}else if(!item.equals("DEFAULT") && item != list.get(0)){
					strBuilder.append("module ");
				}
				strBuilder.append(item);
			}
			strBuilder.append("\n");
		}
		
		return strBuilder.toString();
	}
	
	public void insertIntoStringStack(String str){
		stringOuterStack.get(outerScopeCounter).add(str);
	}
	
	public void incrementOuterScopeCounter(){
		stringOuterStack.add(new Stack<String>());
		outerScopeCounter++;
		operationFlag = false;
		functionFlag = false;
	}

	@Override
	public void setInsTable(ISymbolTable it) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String createNewReturnValue(INode node, IndentTracker question) throws AnalysisException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String createNewReturnValue(Object node, IndentTracker question) throws AnalysisException {
		// TODO Auto-generated method stub
		return null;
	}
}