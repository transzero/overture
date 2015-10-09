package org.overture.codegen.vdm2jml;

import java.util.LinkedList;
import java.util.List;

import org.overture.codegen.cgast.INode;
import org.overture.codegen.cgast.SExpCG;
import org.overture.codegen.cgast.SStmCG;
import org.overture.codegen.cgast.STypeCG;
import org.overture.codegen.cgast.analysis.AnalysisException;
import org.overture.codegen.cgast.declarations.ADefaultClassDeclCG;
import org.overture.codegen.cgast.declarations.AFieldDeclCG;
import org.overture.codegen.cgast.declarations.AFormalParamLocalParamCG;
import org.overture.codegen.cgast.declarations.AMethodDeclCG;
import org.overture.codegen.cgast.declarations.AVarDeclCG;
import org.overture.codegen.cgast.expressions.AIdentifierVarExpCG;
import org.overture.codegen.cgast.expressions.SVarExpCG;
import org.overture.codegen.cgast.patterns.AIdentifierPatternCG;
import org.overture.codegen.cgast.statements.AAssignToExpStmCG;
import org.overture.codegen.cgast.statements.ABlockStmCG;
import org.overture.codegen.cgast.statements.ACallObjectExpStmCG;
import org.overture.codegen.cgast.statements.AMapSeqUpdateStmCG;
import org.overture.codegen.cgast.statements.AMetaStmCG;
import org.overture.codegen.cgast.statements.AReturnStmCG;
import org.overture.codegen.ir.IRGeneratedTag;
import org.overture.codegen.ir.IRInfo;
import org.overture.codegen.logging.Logger;
import org.overture.codegen.trans.assistants.TransAssistantCG;

public class NamedTypeInvHandler
{
	public static final String RET_VAR_NAME_PREFIX = "ret_";
	public static final String MAP_SEQ_NAME_PREFIX = "col_";

	private InvAssertionTrans invTrans;
	private NamedTypeInvUtil util;
	
	public InvAssertionTrans getInvAssertTrans()
	{
		return invTrans;
	}
	
	public NamedTypeInvHandler(InvAssertionTrans invTrans)
	{
		this.invTrans = invTrans;
		this.util = new NamedTypeInvUtil(this);
	}

	public void handleClass(ADefaultClassDeclCG node) throws AnalysisException
	{
		// We want only to treat fields and methods specified by the user.
		// This case helps us avoiding visiting invariant methods

		for (AFieldDeclCG f : node.getFields())
		{
			f.apply(invTrans);
		}

		for (AMethodDeclCG m : node.getMethods())
		{
			m.apply(invTrans);
		}
	}
	
	public void handleField(AFieldDeclCG node)
	{
		/**
		 * Values and record fields will be handled by this handler (not the state component field since its type is a
		 * record type) Example: val : char | Even = 5;
		 */
		ADefaultClassDeclCG encClass = invTrans.getJmlGen().getUtil().getEnclosingClass(node);

		if (encClass == null)
		{
			return;
		}

		if (!invTrans.getRecInfo().isRecField(node) && node.getFinal())
		{
			/**
			 * So at this point it must be a value defined in a module. No need to check if invariant checks are enabled.
			 */
			
			List<AbstractTypeInfo> invTypes = util.findTypeInfo(node.getType());

			if (!invTypes.isEmpty())
			{
				AIdentifierVarExpCG var = getJmlGen().getJavaGen().getInfo().getExpAssistant().consIdVar(node.getName(), node.getType().clone());
				
				List<String> invStrings = util.consJmlCheck(encClass.getName(), JmlGenerator.JML_PUBLIC, JmlGenerator.JML_STATIC_INV_ANNOTATION, false, invTypes, var);
				for(String invStr : invStrings)
				{
					getAnnotator().appendMetaData(node, getAnnotator().consMetaData(invStr));
				}
			}
			else
			{
				// Since value definitions can only be initialised with literals there is no
				// need to guard against null (see JmlGenerator.initialIRConstructed)
//				if(varMayBeNull(node.getType()) && rightHandSideMayBeNull(node.getInitial()))
//				{
//					getAnnotator().appendMetaData(node, util.consValNotNullInvariant(node.getName()));
//				}
			}
			
		}
		/**
		 * No need to assert type consistency of record fields since this is handled by the record setter
		 */
	}

	private JmlAnnotationHelper getAnnotator()
	{
		return invTrans.getJmlGen().getAnnotator();
	}

	public void handleBlock(ABlockStmCG node) throws AnalysisException
	{
		if (node.getLocalDefs().size() > 1)
		{
			LinkedList<AVarDeclCG> origDecls = new LinkedList<AVarDeclCG>(node.getLocalDefs());

			for (int i = origDecls.size() - 1; i >= 0; i--)
			{
				AVarDeclCG nextDecl = origDecls.get(i);

				ABlockStmCG block = new ABlockStmCG();
				block.getLocalDefs().add(nextDecl);

				node.getStatements().addFirst(block);
			}

			for (SStmCG stm : node.getStatements())
			{
				stm.apply(invTrans);
			}

		} else
		{
			if(!node.getLocalDefs().isEmpty())
			{
				node.getLocalDefs().getFirst().apply(invTrans);
			}

			for (SStmCG stm : node.getStatements())
			{
				stm.apply(invTrans);
			}
		}
	}
	
	public void handleReturn(AReturnStmCG node) throws AnalysisException
	{
		/**
		 * The idea is to extract the return value to variable and return that variable. Then it becomes the
		 * responsibility of the variable declaration case to assert if the named invariant type is violated.
		 */
		SExpCG exp = node.getExp();
		
		AMethodDeclCG encMethod = invTrans.getJmlGen().getUtil().getEnclosingMethod(node);

		if (encMethod == null)
		{
			return;
		}

		STypeCG returnType = encMethod.getMethodType().getResult();

		List<AbstractTypeInfo> invTypes = util.findTypeInfo(returnType);

		if (invTypes.isEmpty())
		{
			return;
		}

		String name = getInfo().getTempVarNameGen().nextVarName(RET_VAR_NAME_PREFIX);
		AIdentifierPatternCG id = getInfo().getPatternAssistant().consIdPattern(name);

		AIdentifierVarExpCG varExp = getInfo().getExpAssistant().consIdVar(name, returnType.clone());
		getTransAssist().replaceNodeWith(exp, varExp);

		AVarDeclCG varDecl = getInfo().getDeclAssistant().consLocalVarDecl(returnType.clone(), id, exp.clone());
		ABlockStmCG replBlock = new ABlockStmCG();
		replBlock.getLocalDefs().add(varDecl);

		getTransAssist().replaceNodeWith(node, replBlock);

		replBlock.getStatements().add(node);
		varDecl.apply(invTrans);
	}

	public void handleMethod(AMethodDeclCG node) throws AnalysisException
	{
		if(!treatMethod(node))
		{
			return;
		}
		
		// Upon entering the method, assert that the parameters are valid wrt. their named invariant types.
		
		ABlockStmCG replBody = new ABlockStmCG();
		for (AFormalParamLocalParamCG param : node.getFormalParams())
		{
			List<AbstractTypeInfo> invTypes = util.findTypeInfo(param.getType());

			if (!invTypes.isEmpty())
			{
				ADefaultClassDeclCG encClass = invTrans.getJmlGen().getUtil().getEnclosingClass(node);

				if (encClass == null)
				{
					continue;
				}

				String encClassName = encClass.getName();

				String varNameStr = invTrans.getJmlGen().getUtil().getName(param.getPattern());

				if (varNameStr == null)
				{
					continue;
				}
				
				SVarExpCG var = getInfo().getExpAssistant().consIdVar(varNameStr, param.getType().clone());

				/**
				 * Upon entering a record setter it is necessary to check if invariants checks are enabled before
				 * checking the parameter
				 */
				List<AMetaStmCG> as = util.consAssertStm(invTypes, encClassName, var, node, invTrans.getRecInfo());
				for(AMetaStmCG a : as)
				{
					replBody.getStatements().add(a);
				}
			}
		}

		SStmCG body = node.getBody();
		getTransAssist().replaceNodeWith(body, replBody);
		replBody.getStatements().add(body);
		body.apply(invTrans);
	}

	public List<AMetaStmCG> handleMapSeq(AMapSeqUpdateStmCG node)
	{
		// TODO: Consider this for the atomic statement

		SExpCG col = node.getCol();

		if(!(col instanceof SVarExpCG))
		{
			Logger.getLog().printErrorln("Expected collection to be a variable expression at this point. Got: "
					+ col + " in '" + this.getClass().getSimpleName()
					+ "'");
		}
		
		SVarExpCG var = ((SVarExpCG) col);
		
		if (varMayBeNull(var.getType()))
		{
			// The best we can do is to assert that the map/seq subject to modification is
			// not null although we eventually get the null pointer exception, e.g.
			//
			// //@ azzert m != null
			// Utils.mapSeqUpdate(m,1L,1L);
			AMetaStmCG assertNotNull = util.consVarNotNullAssert(var.getName());

			ABlockStmCG replStm = new ABlockStmCG();
			getTransAssist().replaceNodeWith(node, replStm);
			replStm.getStatements().add(assertNotNull);
			replStm.getStatements().add(node);
		}
		
		List<AbstractTypeInfo> invTypes = util.findTypeInfo(var.getType());
	
		if (!invTypes.isEmpty())
		{
			ADefaultClassDeclCG enclosingClass = invTrans.getJmlGen().getUtil().getEnclosingClass(node);

			if (enclosingClass == null)
			{
				return null;
			}

			if (col instanceof SVarExpCG)
			{
				/**
				 * Updates to fields in record setters need to check if invariants checks are enabled
				 */
				return util.consAssertStm(invTypes, enclosingClass.getName(), var, node,  invTrans.getRecInfo());
			} 
		}
		
		return null;
	}
	
	public List<AMetaStmCG> handleVarDecl(AVarDeclCG node)
	{
		// Examples:
		// let x : Even = 1 in ...
		// (dcl y : Even | nat := 2; ...)

		List<AbstractTypeInfo> invTypes = util.findTypeInfo(node.getType());

		if (!invTypes.isEmpty())
		{
			String name = invTrans.getJmlGen().getUtil().getName(node.getPattern());

			if (name == null)
			{
				return null;
			}

			ADefaultClassDeclCG enclosingClass = node.getAncestor(ADefaultClassDeclCG.class);

			if (enclosingClass == null)
			{
				return null;
			}

			AIdentifierVarExpCG var = getJmlGen().getJavaGen().getInfo().getExpAssistant().consIdVar(name, node.getType().clone());

			/**
			 * We do not really need to check if invariant checks are enabled because local variable declarations are
			 * not expected to be found inside record accessors
			 */
			return util.consAssertStm(invTypes, enclosingClass.getName(), var, node, invTrans.getRecInfo());
		}
		
		return null;
	}

	public List<AMetaStmCG> handleCallObj(ACallObjectExpStmCG node)
	{
		/**
		 * Handling of setter calls to masked records. This will happen for cases like T = R ... ; R :: x : int;
		 */
		SExpCG recObj = node.getObj();

		if (recObj instanceof SVarExpCG)
		{
			SVarExpCG recObjVar = (SVarExpCG) recObj;

			if(varMayBeNull(recObj.getType()))
			{
				// The best we can do is to assert that the record subject to modification is
				// not null although we eventually get the null pointer exception, e.g.
				//
				// //@ azzert rec != null
				// rec.set_x(5);
				AMetaStmCG assertNotNull = util.consVarNotNullAssert(recObjVar.getName());
				
				ABlockStmCG replStm = new ABlockStmCG();
				getTransAssist().replaceNodeWith(node, replStm);
				replStm.getStatements().add(assertNotNull);
				replStm.getStatements().add(node);
				
				return null;
			}
			
			List<AbstractTypeInfo> invTypes = util.findTypeInfo(recObj.getType());

			if (!invTypes.isEmpty())
			{
				ADefaultClassDeclCG encClass = invTrans.getJmlGen().getUtil().getEnclosingClass(node);

				if (encClass == null)
				{
					return null;
				}

				/**
				 * Since setter calls can occur inside a record in the context of an atomic statement blocks we need to
				 * check if invariant checks are enabled
				 */
				return util.consAssertStm(invTypes, encClass.getName(), recObjVar, node, invTrans.getRecInfo());
			}
		}
		else
		{
			Logger.getLog().printErrorln("Found unexpected record object of call expression "
					+ " statement inside atomic statement block in '"
					+ this.getClass().getSimpleName() + "'. Target found: "
					+ recObj);
		}
		
		return null;
	}
	
	public void handleAssign(AAssignToExpStmCG node)
	{
		// <target> := atomic_tmp;

		/*
		 * Note that assignment to targets that are of type AFieldNumberExpCG, i.e. tuples (e.g. tup.#1 := 5) is not
		 * allowed in VDM.
		 */
		
		SExpCG target = node.getTarget();

		if (!(target instanceof SVarExpCG))
		{
			Logger.getLog().printErrorln("By now all assignments should have simple variable expression as target. Got: "
					+ target);
			return;
		}

		SVarExpCG var = (SVarExpCG) target;
		
		List<AbstractTypeInfo> invTypes = util.findTypeInfo(node.getTarget().getType());

		if (!invTypes.isEmpty())
		{
			ADefaultClassDeclCG encClass = invTrans.getJmlGen().getUtil().getEnclosingClass(node);
			
			if (encClass == null)
			{
				return;
			}
			
			/**
			 * Since assignments can occur inside record setters in the context of an atomic statement block we need to
			 * check if invariant checks are enabled
			 */
			List<AMetaStmCG> asserts = util.consAssertStm(invTypes, encClass.getName(), var, node, invTrans.getRecInfo());
			
			for(AMetaStmCG a : asserts)
			{
				addAssert(node, a);
			}
		}
	}

	public ABlockStmCG getEncBlockStm(AVarDeclCG varDecl)
	{
		if (varDecl.parent() instanceof ABlockStmCG)
		{
			ABlockStmCG parentBlock = (ABlockStmCG) varDecl.parent();

			if (!parentBlock.getLocalDefs().contains(varDecl))
			{
				Logger.getLog().printErrorln("Expected local variable declaration to be "
						+ "one of the local variable declarations of "
						+ "the parent statement block in '"
						+ this.getClass().getSimpleName() + "'");
				return null;
			}

			if (parentBlock.getLocalDefs().size() > 1)
			{
				// The block statement case method should have ensured that the size == 1
				Logger.getLog().printErrorln("Expected only a single local declaration in "
						+ "the parent block at this point in '"
						+ this.getClass().getSimpleName() + "'");
				return null;
			}
			
			return parentBlock;
		}
		else
		{
			Logger.getLog().printErrorln("Expected parent of local variable "
					+ "declaration to be a statement block. Got: "
					+ varDecl.parent() + " in '" + this.getClass().getSimpleName()
					+ "'");
			return null;
		}
	}
	
	private TransAssistantCG getTransAssist()
	{
		return invTrans.getJmlGen().getJavaGen().getTransAssistant();
	}

	private IRInfo getInfo()
	{
		return invTrans.getJmlGen().getJavaGen().getInfo();
	}
	
	public JmlGenerator getJmlGen()
	{
		return invTrans.getJmlGen();
	}

	public List<AMetaStmCG> consAsserts(AIdentifierVarExpCG var)
	{
		List<AbstractTypeInfo> invTypes = util.findTypeInfo(var.getType());

		if (invTypes.isEmpty())
		{
			return null;
		}

		ADefaultClassDeclCG encClass = invTrans.getStateDesInfo().getEnclosingClass(var);

		if (encClass == null)
		{
			return null;
		}
		
		/**
		 * Normalisation of state designators will never occur inside record classes so really there is no need to check
		 * if invariant checks are enabled
		 */
		return util.consAssertStm(invTypes, encClass.getName(), var, var, invTrans.getRecInfo());
	}
	
	public boolean rightHandSideMayBeNull(SExpCG exp)
	{
		IsValChecker checker = new IsValChecker();
		
		try
		{
			return !exp.apply(checker);
		} catch (AnalysisException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	private boolean varMayBeNull(STypeCG type)
	{
		return treatMethod(type) && !getInfo().getTypeAssistant().allowsNull(type);
	}

	private boolean treatMethod(INode node)
	{
		if(inModuleToStringMethod(node))
		{
			return false;
		}
		
		// Some of the record methods inherited from object use native java type that can never be null
		
		if(invTrans.getRecInfo().inRec(node))
		{
			if(!(invTrans.getRecInfo().inAccessor(node) || invTrans.getRecInfo().inRecConstructor(node)))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private boolean inModuleToStringMethod(INode type)
	{
		AMethodDeclCG m = type.getAncestor(AMethodDeclCG.class);
		
		if(m == null)
		{
			return false;
		}
		
		if(m.getTag() instanceof IRGeneratedTag && m.getName().equals("toString"))
		{
			return true;
		}
		
		return false;
	}

	private void addAssert(AAssignToExpStmCG node, AMetaStmCG assertStm)
	{
		ABlockStmCG replStm = new ABlockStmCG();
		getJmlGen().getJavaGen().getTransAssistant().replaceNodeWith(node, replStm);
		replStm.getStatements().add(node);
		replStm.getStatements().add(assertStm);
	}
}
