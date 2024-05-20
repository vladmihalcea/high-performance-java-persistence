package com.vladmihalcea.hpjp.spring.stateless.hibernate;

import com.vladmihalcea.hpjp.spring.stateless.domain.BatchInsertPost;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.internal.MutationExecutorServiceInitiator;
import org.hibernate.engine.jdbc.mutation.internal.MutationExecutorSingleBatched;
import org.hibernate.engine.jdbc.mutation.internal.StandardMutationExecutorService;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.*;

/**
 * Custom MutationExecutorService implementation that allows batch insertion for Identity Generator
 *
 * @author Fernando Silva
 * @see MutationExecutorServiceInitiator
 */
public class CustomMutationExecutorService extends StandardMutationExecutorService {
    //private field in StandardMutationExecutorService
    private final int globalBatchSize;

    //default constructor used by Hibernate
    public CustomMutationExecutorService() {
        super(8);
        this.globalBatchSize = 8;
    }

    @Override
    public MutationExecutor createExecutor(
            BatchKeyAccess batchKeySupplier,
            MutationOperationGroup operationGroup,
            SharedSessionContractImplementor session) {

        int batchSizeToUse = session.getJdbcCoordinator().getJdbcSessionOwner().getJdbcBatchSize() == null ? globalBatchSize : session.getJdbcCoordinator().getJdbcSessionOwner().getJdbcBatchSize();

        //only uses batch for BatachInsert
        if (isSingleInsertOperationForBatchInsert(operationGroup)) {
            PreparableMutationOperation jdbcOperation = (PreparableMutationOperation) operationGroup.getSingleOperation();
            //batch key is not set in batchKeySupplier because it's a generated id
            BatchKey batchKey = new BasicBatchKey(BatchInsertPost.class + "#INSERT", null);
            return new MutationExecutorSingleBatched(jdbcOperation, batchKey, batchSizeToUse, session);
        }

        return super.createExecutor(batchKeySupplier, operationGroup, session);
    }

    private boolean isSingleInsertOperationForBatchInsert(MutationOperationGroup operationGroup) {
        if (operationGroup.getNumberOfOperations() != 1) {
            return false;
        }
        MutationType mutationType = operationGroup.getMutationType();
        EntityMutationOperationGroup entityMutationOperationGroup = operationGroup.asEntityMutationOperationGroup();
        return entityMutationOperationGroup != null && mutationType == MutationType.INSERT && entityMutationOperationGroup.getMutationTarget().getTargetPart().getJavaType().getJavaTypeClass().isAssignableFrom(BatchInsertPost.class);
    }

}
