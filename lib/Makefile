CC=gcc
SOURCE_DIR=src
OUT_DIR=out
INCLUDE=-Iinc -Iinc/graphviz

ifeq ($(OS), Windows_NT)
	LIB_FLAGS=-Lbin/windows -lgvc -lcgraph -lcdt
	LIB_OUTPUT=gw.dll
else
	LIB_FLAGS=-Lbin/linux -lgvc -lcgraph -lcdt
	LIB_OUTPUT=libgw.so
endif

C_FLAGS=-g $(INCLUDE) -Wall

.PHONY: all
.PHONY: lib
.PHONY: test
.PHONY: clean

all:
	make lib
	make test

lib: wrapper.o
	$(CC) -shared $(C_FLAGS) $(OUT_DIR)/$^ $(LIB_FLAGS) -o $(LIB_OUTPUT)

test:
	$(CC) $(C_FLAGS) -DTEST $(SOURCE_DIR)/wrapper.c $(LIB_FLAGS) -o $@

clean:
	rm -rf $(OUT_DIR)/*.o
	rm -rf test
	rm -rf $(LIB_OUTPUT)

%.o: $(SOURCE_DIR)/%.c
	$(CC) $(C_FLAGS) $(LIB_FLAGS) -c $^ -o $(OUT_DIR)/$@
