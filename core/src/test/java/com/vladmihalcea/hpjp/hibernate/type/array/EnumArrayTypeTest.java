package com.vladmihalcea.hpjp.hibernate.type.array;

import com.vladmihalcea.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.array.EnumArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumArrayTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Sudoku.class,
        };
    }

    @Override
    protected void beforeInit() {
        executeStatement("DROP TYPE IF EXISTS sudoku_state");
        executeStatement("CREATE TYPE sudoku_state AS ENUM ('POSSIBLE', 'IMPOSSIBLE', 'UNDEFINED', 'UNKNOWN')");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Sudoku sudoku = new Sudoku();
            sudoku.setId(1L);
            entityManager.persist(sudoku);

            sudoku.setStateValues(new SudokuPossibleValueState[] {
                SudokuPossibleValueState.POSSIBLE,
                SudokuPossibleValueState.IMPOSSIBLE,
                SudokuPossibleValueState.POSSIBLE,
                SudokuPossibleValueState.POSSIBLE,
                SudokuPossibleValueState.POSSIBLE,
                SudokuPossibleValueState.UNDEFINED,
                SudokuPossibleValueState.POSSIBLE,
                SudokuPossibleValueState.POSSIBLE,
                SudokuPossibleValueState.UNKNOWN,
            });
        });
        doInJPA(entityManager -> {
            Sudoku sudoku = entityManager.find(Sudoku.class, 1L);

            assertEquals( 9L, sudoku.getStateValues().length );
        });
    }

    @Entity(name = "Sudoku")
    @Table(name = "sudoku")
    public static class Sudoku extends BaseEntity {

        @Type(
            value = EnumArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                name = "sql_array_type",
                value = "sudoku_state"
            )
        )
        @Column(name = "sensor_values", columnDefinition = "sudoku_state[]")
        private SudokuPossibleValueState[] stateValues;

        public SudokuPossibleValueState[] getStateValues() {
            return stateValues;
        }

        public void setStateValues(SudokuPossibleValueState[] stateValues) {
            this.stateValues = stateValues;
        }
    }

    public enum SudokuPossibleValueState {
        UNDEFINED, UNKNOWN, IMPOSSIBLE, POSSIBLE, COMMITTED, AS_PUBLISHED;
    }
}
