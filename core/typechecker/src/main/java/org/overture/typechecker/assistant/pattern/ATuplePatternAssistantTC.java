package org.overture.typechecker.assistant.pattern;

import org.overture.typechecker.assistant.ITypeCheckerAssistantFactory;

public class ATuplePatternAssistantTC
{
	protected static ITypeCheckerAssistantFactory af;

	@SuppressWarnings("static-access")
	public ATuplePatternAssistantTC(ITypeCheckerAssistantFactory af)
	{
		this.af = af;
	}

//	public static void typeResolve(ATuplePattern pattern,
//			IQuestionAnswer<TypeCheckInfo, PType> rootVisitor,
//			TypeCheckInfo question) throws AnalysisException
//	{
//
//		if (pattern.getResolved())
//		{
//			return;
//		} else
//		{
//			pattern.setResolved(true);
//		}
//
//		try
//		{
//			PPatternListAssistantTC.typeResolve(pattern.getPlist(), rootVisitor, question);
//		} catch (TypeCheckException e)
//		{
//			unResolve(pattern);
//			throw e;
//		}
//
//	}
//
//	public static void unResolve(ATuplePattern pattern)
//	{
//
//		PPatternListAssistantTC.unResolve(pattern.getPlist());
//		pattern.setResolved(false);
//
//	}

//	public static List<PDefinition> getAllDefinitions(ATuplePattern rp,
//			PType type, NameScope scope)
//	{
//
//		List<PDefinition> defs = new Vector<PDefinition>();
//
//		if (!PTypeAssistantTC.isProduct(type, rp.getPlist().size()))
//		{
//			TypeCheckerErrors.report(3205, "Matching expression is not a product of cardinality "
//					+ rp.getPlist().size(), rp.getLocation(), rp);
//			TypeCheckerErrors.detail("Actual", type);
//			return defs;
//		}
//
//		AProductType product = PTypeAssistantTC.getProduct(type, rp.getPlist().size());
//		Iterator<PType> ti = product.getTypes().iterator();
//
//		for (PPattern p : rp.getPlist())
//		{
//			defs.addAll(PPatternAssistantTC.getDefinitions(p, ti.next(), scope));
//		}
//
//		return defs;
//	}

}
