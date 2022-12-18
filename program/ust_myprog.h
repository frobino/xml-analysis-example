#undef LTTNG_UST_TRACEPOINT_PROVIDER
#define LTTNG_UST_TRACEPOINT_PROVIDER ust_myprog

#undef LTTNG_UST_TRACEPOINT_INCLUDE
#define LTTNG_UST_TRACEPOINT_INCLUDE "./ust_myprog.h"

#if !defined(_UST_MYPROG_H) || defined(LTTNG_UST_TRACEPOINT_HEADER_MULTI_READ)
#define _UST_MYPROG_H

#include <lttng/tracepoint.h>

LTTNG_UST_TRACEPOINT_EVENT(
	ust_myprog,
	connection_start,
    LTTNG_UST_TP_ARGS(
        int, my_integer_arg
    ),
    LTTNG_UST_TP_FIELDS(
        lttng_ust_field_integer(int, my_integer_field, my_integer_arg)
    )
)

LTTNG_UST_TRACEPOINT_EVENT(ust_myprog, connection_end,
    LTTNG_UST_TP_ARGS(
        int, my_integer_arg
    ),
    LTTNG_UST_TP_FIELDS(
        lttng_ust_field_integer(int, my_integer_field, my_integer_arg)
    )
)

LTTNG_UST_TRACEPOINT_EVENT(ust_myprog, connection_wait,
    LTTNG_UST_TP_ARGS(
        int, my_integer_arg
    ),
    LTTNG_UST_TP_FIELDS(
        lttng_ust_field_integer(int, my_integer_field, my_integer_arg)
    )
)

#endif /* _UST_MYPROG_H */

/* This part must be outside ifdef protection */
#include <lttng/tracepoint-event.h>
