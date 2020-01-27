
NAME=helmet
BINDIR ?= /usr/local/bin
OUTPUT=target/$(NAME)

SRCS += $(shell find src -type f)

all: bin

bin: $(OUTPUT)

$(OUTPUT): $(SRCS) Makefile project.clj
	@lein bin

$(PREFIX)$(BINDIR):
	mkdir -p $@

install: $(OUTPUT) $(PREFIX)$(BINDIR)
	cp $(OUTPUT) $(PREFIX)$(BINDIR)

clean:
	@echo "Cleaning up.."
	@lein clean
	-@rm -rf target
	-@rm -f *~
