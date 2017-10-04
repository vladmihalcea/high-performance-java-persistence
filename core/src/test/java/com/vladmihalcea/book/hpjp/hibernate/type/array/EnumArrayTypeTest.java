package com.vladmihalcea.book.hpjp.hibernate.type.array;

import com.vladmihalcea.book.hpjp.hibernate.type.json.model.BaseEntity;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import static org.junit.Assert.assertArrayEquals;
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
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return PostgreSQL95ArrayDialect.class.getName();
            }
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

        @Type( type = "enum-array" )
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
