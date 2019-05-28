/*
 * Copyright (c) 2019, Mihalcea Vlad-Alexandru (https://vladmihalcea.com)
 * All rights reserved.
 *
 * Mihalcea Vlad-Alexandru grants the Customer the non-exclusive,
 * timely limited and non-transferable license to install and use the Software
 * under the terms of the Hypersistence Optimizer License Agreement.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * See the Hypersistence Optimizer License Agreement for more details:
 *
 * https://vladmihalcea.com/hypersistence-optimizer/license
 */
package com.vladmihalcea.book.hpjp.hibernate.logging;

import com.vladmihalcea.book.hpjp.util.StackTraceUtils;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link com.vladmihalcea.book.hpjp.hibernate.logging.LoggingStatementInspector}
 *
 * @author Vlad Mihalcea
 * @since 1.x.y
 */
public class LoggingStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStatementInspector.class);

    private final String packageNamePrefix;

    public LoggingStatementInspector(String packageNamePrefix) {
        this.packageNamePrefix = packageNamePrefix;
    }

    @Override
    public String inspect(String sql) {
        LOGGER.info(
                "Executing SQL query: {} from {}",
                sql,
                StackTraceUtils.stackTracePath(
                    StackTraceUtils.stackTraceElements(
                        packageNamePrefix
                    )
                )
        );
        return null;
    }
}
