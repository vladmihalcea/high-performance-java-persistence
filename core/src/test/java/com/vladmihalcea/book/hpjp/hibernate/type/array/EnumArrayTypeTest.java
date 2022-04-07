package com.vladmihalcea.book.hpjp.hibernate.type.array;

import com.vladmihalcea.book.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hibernate.type.array.EnumArrayType;
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

        @Type(EnumArrayType.class)
        @Column(name = "sensor_values", columnDefinition = "integer[]")
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
