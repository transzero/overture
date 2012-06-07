package org.overture.ast.statements.assistants;

import org.overture.ast.statements.AElseIfStm;
import org.overture.ast.statements.AIfStm;
import org.overture.ast.utils.PTypeSet;

public class AIfStmAssistantTC {

	public static PTypeSet exitCheck(AIfStm statement) {
		
		PTypeSet types = new PTypeSet();
		types.addAll(PStmAssistantTC.exitCheck(statement.getThenStm()));

		for (AElseIfStm stmt: statement.getElseIf())
		{
			types.addAll(AElseIfStmAssistantTC.exitCheck(stmt));
		}

		if (statement.getElseStm() != null)
		{
			types.addAll(PStmAssistantTC.exitCheck(statement.getElseStm()));
		}

		return types;
	}

}
