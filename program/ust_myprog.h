#undef TRACEPOINT_PROVIDER
#define TRACEPOINT_PROVIDER ust_myprog

#if !defined(_TRACEPOINT_UST_MYPROG_H) || defined(TRACEPOINT_HEADER_MULTI_READ)
#define _TRACEPOINT_UST_MYPROG_H

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Copyright (C) 2011  Mathieu Desnoyers <mathieu.desnoyers@efficios.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */

#include <lttng/tracepoint.h>
#include <stdbool.h>

TRACEPOINT_EVENT(ust_myprog, connection_start,
	TP_ARGS(int, anint),
	TP_FIELDS(
		ctf_integer(int, id, anint)
	)
)

TRACEPOINT_EVENT(ust_myprog, connection_end,
	TP_ARGS(int, anint),
	TP_FIELDS(
		ctf_integer(int, id, anint)
	)
)

#endif /* _TRACEPOINT_UST_MYPROG_H */

#undef TRACEPOINT_INCLUDE_FILE
#define TRACEPOINT_INCLUDE_FILE ./ust_myprog.h

/* This part must be outside ifdef protection */
#include <lttng/tracepoint-event.h>

#ifdef __cplusplus 
}
#endif
