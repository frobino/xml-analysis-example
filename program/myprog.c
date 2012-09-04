/*
 * Copyright (C) 2009  Pierre-Marc Fournier
 * Copyright (C) 2011  Mathieu Desnoyers <mathieu.desnoyers@efficios.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; version 2.1 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

#include <stdio.h>
#include <stdlib.h>
#include <omp.h>

#define TRACEPOINT_DEFINE
#include "ust_myprog.h"


int main(int argc, char **argv)
{
	int nb_threads = 5;
	int i;

	srand(time(NULL));

	fprintf(stderr, "Tracing...\n");

	#pragma omp parallel private(i) num_threads(nb_threads)
	for (i = 0; i < 10; i++) {
		int delay1 = rand() % 50000;
		int delay2 = rand() % 50000;
		int delay3 = rand() % 50000;
		int id = omp_get_thread_num() + 1;

		/* Loop starts here */

		usleep(delay1);

		//Connection attempted

		usleep(delay2);

		//Connection is established
		tracepoint(ust_myprog, connection_start, id);

		usleep(delay3);

		//Connection ends
		tracepoint(ust_myprog, connection_end, id);
	}
	
	fprintf(stderr, "Done.\n");
	return 0;
}



















