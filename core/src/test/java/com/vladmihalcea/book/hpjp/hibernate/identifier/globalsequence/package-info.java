@GenericGenerator(
	name = "pooled",
	strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
	parameters = {
		@Parameter(name = "sequence_name", value = "sequence"),
		@Parameter(name = "initial_value", value = "1"),
		@Parameter(name = "increment_size", value = "5"),
		@Parameter(name = "optimizer", value = "pooled-lo"),
	}
)
package com.vladmihalcea.book.hpjp.hibernate.identifier.globalsequence;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;