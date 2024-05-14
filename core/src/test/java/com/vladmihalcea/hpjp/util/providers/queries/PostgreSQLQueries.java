package com.vladmihalcea.hpjp.util.providers.queries;

import com.vladmihalcea.hpjp.jdbc.index.PostgreSQLIndexSelectivityTest;
import org.postgresql.PGStatement;
import org.postgresql.util.PGobject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLQueries implements Queries {

    public static final Queries INSTANCE = new PostgreSQLQueries();

    @Override
    public String transactionId() {
        return "SELECT CAST(pg_current_xact_id_if_assigned() AS text)";
    }

    public static void setPrepareThreshold(Statement statement, int threshold) throws SQLException {
        if(statement instanceof PGStatement) {
            PGStatement pgStatement = (PGStatement) statement;
            pgStatement.setPrepareThreshold(threshold);
        } else {
            InvocationHandler handler = Proxy.getInvocationHandler(statement);
            try {
                handler.invoke(statement, PGStatement.class.getMethod("setPrepareThreshold", int.class), new Object[]{threshold});
            } catch (Throwable throwable) {
                throw new IllegalArgumentException(throwable);
            }
        }
    }

    public static boolean isUseServerPrepare(Statement statement) {
        if(statement instanceof PGStatement) {
            PGStatement pgStatement = (PGStatement) statement;
            return pgStatement.isUseServerPrepare();
        } else {
            InvocationHandler handler = Proxy.getInvocationHandler(statement);
            try {
                return (boolean) handler.invoke(statement, PGStatement.class.getMethod("isUseServerPrepare"), null);
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public static PGobject toEnum(Enum enumValue, String enumName) throws SQLException {
        PGobject object = new PGobject();
        object.setType(enumName);
        object.setValue(enumValue.name());
        return object;
    }
}
