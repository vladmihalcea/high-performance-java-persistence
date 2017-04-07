package com.vladmihalcea.book.hpjp.util.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.DefaultQueryLogEntryCreator;

/**
 * @author Vlad Mihalcea
 */
public class InlineQueryLogEntryCreator extends DefaultQueryLogEntryCreator {
	@Override
	protected void writeParamsEntry(StringBuilder sb, ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
		sb.append( "Params:[" );
		for ( QueryInfo queryInfo : queryInfoList ) {
			boolean firstArg = true;
			for ( Map<String, Object> paramMap : queryInfo.getQueryArgsList() ) {

				if ( !firstArg ) {
					sb.append( ", " );
				}
				else {
					firstArg = false;
				}

				SortedMap<String, Object> sortedParamMap = new TreeMap<>( new StringAsIntegerComparator() );
				sortedParamMap.putAll( paramMap );

				sb.append( "(" );
				boolean firstParam = true;
				for ( Map.Entry<String, Object> paramEntry : sortedParamMap.entrySet() ) {
					if ( !firstParam ) {
						sb.append( ", " );
					}
					else {
						firstParam = false;
					}
					Object parameter = paramEntry.getValue();
					if ( parameter != null && parameter.getClass().isArray() ) {
						sb.append( arrayToString( parameter ) );
					}
					else {
						sb.append( parameter );
					}
				}
				sb.append( ")" );
			}
		}
		sb.append( "]" );
	}

	private String arrayToString(Object object) {
		if ( object.getClass().isArray() ) {
			if ( object instanceof byte[] ) {
				return Arrays.toString( (byte[]) object );
			}
			if ( object instanceof short[] ) {
				return Arrays.toString( (short[]) object );
			}
			if ( object instanceof char[] ) {
				return Arrays.toString( (char[]) object );
			}
			if ( object instanceof int[] ) {
				return Arrays.toString( (int[]) object );
			}
			if ( object instanceof long[] ) {
				return Arrays.toString( (long[]) object );
			}
			if ( object instanceof float[] ) {
				return Arrays.toString( (float[]) object );
			}
			if ( object instanceof double[] ) {
				return Arrays.toString( (double[]) object );
			}
			if ( object instanceof boolean[] ) {
				return Arrays.toString( (boolean[]) object );
			}
			if ( object instanceof Object[] ) {
				return Arrays.toString( (Object[]) object );
			}
		}
		throw new UnsupportedOperationException( "Arrat type not supported: " + object.getClass() );
	}
}
