CC=gcc
SOURCE_DIR=src
OUT_DIR=out
INCLUDE=-Iinc -Iinc/graphviz
LIB_FLAGS=`pkg-config libgvc --libs`
C_FLAGS=-g $(INCLUDE) -Wall

.PHONY: clean
.PHONY: dll

all:
	make dll
	make test

lib: wrapper.o
	$(CC) -shared $(C_FLAGS) $(OUT_DIR)/$^ $(LIB_FLAGS) -o libgw.so

test:
	$(CC) $(C_FLAGS) -DTEST $(SOURCE_DIR)/wrapper.c $(LIB_FLAGS) -o $@

%.o: $(SOURCE_DIR)/%.c
	$(CC) $(C_FLAGS) $(LIB_FLAGS) -c $^ -o $(OUT_DIR)/$@

clean:
	rm -rf out/*.o
	rm -rf test
	rm -rf libgw.so
