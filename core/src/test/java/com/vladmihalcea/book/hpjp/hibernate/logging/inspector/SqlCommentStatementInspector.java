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
package com.vladmihalcea.book.hpjp.hibernate.logging.inspector;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vlad Mihalcea
 */
public class SqlCommentStatementInspector implements StatementInspector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlCommentStatementInspector.class);

    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("\\/\\*.*?\\*\\/\\s*");

    @Override
    public String inspect(String sql) {
        LOGGER.debug(
            "Executing SQL query: {}",
            sql
        );

        return SQL_COMMENT_PATTERN.matcher(sql).replaceAll("");
    }
}
