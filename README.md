# <h1 id="top">YWW Tools</h1>

A package of my ([Weiwei Yang](http://cs.umd.edu/~wwyang/)'s) various tools (most for NLP). Feel free to email me at <wwyang@cs.umd.edu> with any questions.

* [Check Out](#check_out)
* [Dependencies](#dependencies)
* [Use YWW Tools in Command Line](#command)
* [LDA (Latent Dirichlet Allocation) in Command Line](#lda_cmd)
	* [RTM: Relational Topic Model](#rtm_cmd)
		* [Lex-WSB-RTM: RTM with Lexical Weights and Weighted Stochastic Block Priors](#lex_wsb_rtm_cmd)
		* [Lex-WSB-Med-RTM: Lex-WSB-RTM with Hinge Loss](#lex_wsb_med_rtm_cmd)
	* [SLDA: Supervised LDA](#slda_cmd)
		* [BS-LDA: Binary SLDA](#bs_lda_cmd)
		* [Lex-WSB-BS-LDA: BS-LDA with Lexcial Weights and Weighted Stochastic Block Priors](#lex_wsb_bs_lda_cmd)
		* [Lex-WSB-Med-LDA: Lex-WSB-BS-LDA with Hinge Loss](#lex_wsb_med_lda_cmd)
	* [BP-LDA: LDA with Block Priors](#bp_lda_cmd)
	* [ST-LDA: Single Topic LDA](#st_lda_cmd)
	* [WSB-TM: Weighted Stochastic Block Topic Model](#wsb_tm_cmd)
* [Other Tools in Command Line](#other_cmd)
	* [WSBM: Weighted Stochastic Block Model](#wsbm_cmd)
	* [SCC: Strongly Connected Components](#scc_cmd)
	* [Stoplist](#stoplist_cmd)
	* [Lemmatizer](#lemmatizer_cmd)
	* [POS Tagger](#pos_tagger_cmd)
	* [Stemmer](#stemmer_cmd)
	* [Tokenizer](#tokenizer_cmd)
	* [Corpus Converter](#corpus_converter_cmd)
* [Use YWW Tools Source Code](#code_examples)
* [LDA Code Examples](#lda_code)
	* [RTM](#rtm_code)
		* [Lex-WSB-RTM](#lex_wsb_rtm_code)
		* [Lex-WSB-Med-RTM](#lex_wsb_med_rtm_code)
	* [SLDA](#slda_code)
		* [BS-LDA](#bs_lda_code)
		* [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_code)
		* [Lex-WSB-Med-LDA](#lex_wsb_med_lda_code)
	* [BP-LDA](#bp_lda_code)
	* [ST-LDA](#st_lda_code)
	* [WSB-TM](#wsb_tm_code)
* [Other Code Examples](#other_code)
	* [WSBM](#wsbm_code)
	* [SCC](#scc_code)
	* [English Corpus Preprocessing](#preprocess)
* [Citation](#citation)
* [References](#ref)

## <h2 id="check_out">Check Out</h2>

```
git clone git@github.com:ywwbill/YWWTools.git
```

## <h2 id="dependencies">Dependencies</h2>

- Java 8.
- Maven 3.3.
- Files in `lib/`.

## <h2 id="build">Build</h2>

In the package root directory, run command

```
mvn package
```

You will get `YWWTools-1.jar` and `deps.jar` in `target/`.

For convenience, I assume you

- copy `YWWTools-1.jar` and `deps.jar` to package root directory
- rename `YWWTools-1.jar` to `YWWTools.jar`

## <h2 id="command">Use YWW Tools in Command Line</h2>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool <tool-name> --arg-1 <arg-1-value> --arg-2 <arg-2-value> ... --arg-n <arg-n-value>
```

- **<font size=4>Windows users please replace `YWWTools.jar:deps.jar` with `YWWTools.jar;deps.jar`.</font>**
- Supported `<tool-name>` (case unsensitive) include
	- [LDA](#lda_cmd): Latent Dirichlet allocation. Include a variety of extensions.
	- [WSBM](#wsbm_cmd): Weighted stochastic block model. Find blocks in a network.
	- [SCC](#scc_cmd): Strongly connected components.
	- [Stoplist](#stoplist_cmd): Remove stop words. Support English only, but can support other languages given dictionary.
	- [Lemmatizer](#lemmatizer_cmd): Lemmatize POS-tagged corpus. Support English only, but can support other languages given dictionary.
	- [POS-Tagger](#pos_tagger_cmd): Tag words' POS. Support English only, but can support other languages given trained models.
	- [Stemmer](#stemmer_cmd): Stem words. Support English only.
	- [Tokenizer](#tokenizer_cmd): Tokenize corpus. Support English only, but can support other languages given trained models.
	- [Corpus-Converter](#corpus_converter_cmd): Convert word corpus into indexed corpus (for [LDA](#lda_cmd)) and vice versa.
- You can always use `--help` to see help information of 
	- supported tool names if you don't specify a tool name

		```
		java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --help
		```
	- a specific tool if you specify it (take [LDA](#lda_cmd) as an example)

		```
		java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool LDA --help
		```
- In following command examples, arguments in `{}` (e.g. `{cmd-1|cmd-2|cmd-3}`) denote that one and only one of them should be declared.

## <h2 id="lda_cmd">LDA (Latent Dirichlet Allocation) in Command Line</h2>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model lda --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file>
```

- Implementation of [(Blei et al., 2003)](#lda_cmd).
- Required arguments
	- `<vocab-file>`: Vocabulary file. Each line contains a unique word.
	- `<corpus-file>`: Corpus file in which documents are represented by word indexes and frequencies. Each line contains a document in the following format
	
		```
		<doc-len> <word-type-1>:<frequency-1> <word-type-2>:<frequency-2> ... <word-type-n>:<frequency-n>
		```
	
		`<doc-len>` is the total number of *tokens* in this document. `<word-type-i>` denotes the i-th word in `<vocab-file>`, starting from 0. Words with zero frequency can be omitted.
	- `<model-file>`: Trained model file in JSON format. Read and written by program.
- Optional arguments
	- `--model <model-name>`: The topic model you want to use (default: [LDA](#lda_cmd)). Supported `<model-name>` (case unsensitive) are
		- [LDA](#lda_cmd): Vanilla LDA
		- [RTM](#rtm_cmd): Relational topic model.
			- [Lex-WSB-RTM](#lex_wsb_rtm_cmd): [RTM](#rtm_cmd) with WSB-computed block priors and lexical weights.
			- [Lex-WSB-Med-RTM](#lex_wsb_med_rtm_cmd): [Lex-WSB-RTM](#lex_wsb_rtm_cmd) with hinge loss.
		- [SLDA](#slda_cmd): Supervised [LDA](#lda_cmd). Support multi-class classification.
			- [BS-LDA](#bs_lda_cmd): Binary [SLDA](#slda_cmd).
			- [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_cmd): [BS-LDA](#bs_lda_cmd) with WSB-computed block priors and lexical weights.
			- [Lex-WSB-Med-LDA](#lex_wsb_med_lda_cmd): [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_cmd) with hinge loss.
		- [BP-LDA](#bp_lda_cmd): [LDA](#lda_cmd) with block priors. Blocks are pre-computed.
		- [ST-LDA](#st_lda_cmd): Single topic [LDA](#lda_cmd). Each document can only be assigned to one topic.
		- [WSB-TM](#wsb_tm_cmd): [LDA](#lda_cmd) with block priors. Blocks are computed by [WSBM](#wsbm_cmd).
	- `--test`: Use the model for test (default: false).
	- `--no-verbose`: Stop printing log to console.
	- `--alpha <alpha-value>`: Parameter of Dirichlet prior of document distribution over topics (default: 1.0). Must be a positive real number.
	- `--beta <beta-value>`: Parameter of Dirichlet prior of topic distribution over words (default: 0.1). Must be a positive real number.
	- `--topics <num-topics>`: Number of topics (default: 10). Must be a positive integer.
	- `--iters <num-iters>`: Number of iterations (default: 100). Must be a positive integer.
	- `--update`: Update alpha while sampling (default: false).
	- `--update-int <update-interval>`: Interval of updating alpha (default: 10). Must be a positive integer.
	- `--theta <theta-file>`: File for document distribution over topics. Each line contains a document's topic distribution. Topic weights are separated by space.
	- `--output-topic <topic-file>`: File for showing topics.
	- `--topic-count <topic-count-file>`: File for document-topic counts.
	- `--top-word <num-top-word>`: Number of words to give when showing topics (default: 10). Must be a positive integer.

### <h3 id="rtm_cmd">RTM: Relational Topic Model</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model rtm --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --rtm-train-graph <rtm-train-graph-file>
```

- Implementation of [(Chang and Blei, 2010)](#rtm_ref).
- Jointly models topics and document links.
- Extends [LDA](#lda_cmd).
- Semi-optional arguments
	- `--rtm-train-graph <rtm-train-graph-file>` [optional in test]: Link file for RTM to train. Each line contains an edge in the format `node-1 \t node-2 \t weight`. Node number starts from 0. `weight` must be a non-negative integer. `weight` is either 0 or 1 and is optional. Its default value is 1 if not specified.
	- `--rtm-test-graph <rtm-test-graph-file>` [optional in training]: Link file for RTM to evaluate. Can be the same with RTM train graph. Format is the same as `<rtm-train-graph-file>`.
- Optional arguments
	- `--nu <nu-value>`: Variance of normal priors for weight vectors/matrices in RTM and its extensions (default: 1.0). Must be a positive real number.
	- `--plr-int <compute-PLR-interval>`: Interval of computing predictive link rank (default: 20). Must be a positive integer.
	- `--neg`: Sample negative links (default: false).
	- `--neg-ratio <neg-ratio>`: The ratio of number of negative links to number of positive links (default 1.0). Must be a positive real number.
	- `--pred <pred-file>`: Predicted document link probability matrix file.
	- `--reg <reg-file>`: Doc-doc regression value file.
	- `--directed`: Set all edges directed (default: false).

#### <h4 id="lex_wsb_rtm_cmd">Lex-WSB-RTM: [RTM](#rtm_ref) with Lexical Weights and Weighted Stochastic Block Priors</h4>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools yang.weiwei.Tools --tool lda --model lex-wsb-rtm --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --rtm-train-graph <rtm-train-graph-file>
```

- Extends [RTM](#rtm_cmd).
- Optional arguments
	- `--wsbm-graph <wsbm-graph-file>`: Link file for [WSBM](#cmd) to find blocks. See [WSBM](#wsbm_cmd) for details.
	- `--alpha-prime <alpha-prime-value>`: Parameter of Dirichlet prior of block distribution over topics (default: 1.0). Must be a positive real number.
	- `-a <a-value>`: Parameter of Gamma prior for block link rates (default: 1.0). Must be a positive real number.
	- `-b <b-value>`: Parameter of Gamma prior for block link rates (default: 1.0). Must be a positive real number.
	- `-g <gamma-value>`: Parameter of Dirichlet prior for block distribution (default: 1.0). Must be a positive real number.
	- `--blocks <num-blocks>`: Number of blocks (default: 10). Must be a positive integer.
	- `--output-wsbm <wsbm-output-file>`: File for [WSBM](#wsbm_cmd)-identified blocks. See [WSBM](#wsbm_cmd) for details.
	- `--block-feature`: Include block features in link prediction (default: false).

#### <h4 id="lex_wsb_med_rtm_cmd">Lex-WSB-Med-RTM: [Lex-WSB-RTM](#lex_wsb_rtm_cmd) with Hinge Loss</h4>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model lex-wsb-med-rtm --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --rtm-train-graph <rtm-train-graph-file>
```

- Implementation of [(Yang et al., 2016)](#lex_wsb_med_rtm_ref)
- See [(Zhu et al., 2012) and (Zhu et al., 2014)](#med_lda_ref) for hinge loss.
- Extends [Lex-WSB-RTM](#lex_wsb_rtm_cmd).
- Link weight is either 1 or -1.
- Optional arguments
	- `-c <c-value>`: Regularization parameter in hinge loss (default: 1.0). Must be a positive real number.

### <h3 id="slda_cmd">SLDA: Supervised [LDA](#lda_cmd)</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model slda --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --label <label-file>
```

- Implementation of [(McAuliffe and Blei, 2008)](#slda_ref).
- Jointly models topics and document labels. Support multi-class classification.
- Extends [LDA](#lda_cmd).
- Semi-optional arguments
	- `--label <label-file>` [optional in test]: Label file. Each line contains corresponding document's numeric label. If a document label is not available, leave the corresponding line empty.
- Optional arguments
	- `--sigma <sigma-value>`: Variance for the Gaussian generation of response variable in SLDA (default: 1.0). Must be a positive real number.
	- `--nu <nu-value>`: Variance of normal priors for weight vectors in SLDA and its extensions (default: 1.0). Must be a positive real number.
	- `--pred <pred-file>`: Predicted label file.
	- `--reg <reg-file>`: Regression value file.

#### <h4 id="bs_lda_cmd">BS-LDA: Binary [SLDA](#slda_ref)</h4>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model bs-lda --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --label <label-file>
```

- For binary classification only.
- Extends [SLDA](#slda_cmd).
- Label is either 1 or 0.

#### <h4 id="lex_wsb_bs_lda_cmd">Lex-WSB-BS-LDA: [BS-LDA](#bs_lda_cmd) with Lexcial Weights and Weighted Stochastic Block Priors</h4>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model lex-wsb-bs-lda --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --label <label-file>
```

- Extends [BS-LDA](#bs_lda_cmd).
- Optional arguments
	- `--wsbm-graph <wsbm-graph-file>`: Link file for [WSBM](#cmd) to find blocks. See [WSBM](#wsbm_cmd) for details.
	- `--alpha-prime <alpha-prime-value>`: Parameter of Dirichlet prior of block distribution over topics (default: 1.0). Must be a positive real number.
	- `-a <a-value>`: Parameter of Gamma prior for block link rates (default: 1.0). Must be a positive real number.
	- `-b <b-value>`: Parameter of Gamma prior for block link rates (default: 1.0). Must be a positive real number.
	- `-g <gamma-value>`: Parameter of Dirichlet prior for block distribution (default: 1.0). Must be a positive real number.
	- `--blocks <num-blocks>`: Number of blocks (default: 10). Must be a positive integer.
	- `--directed`: Set all edges directed (default: false).
	- `--output-wsbm <wsbm-output-file>`: File for [WSBM](#wsbm_cmd)-identified blocks. See [WSBM](#wsbm_cmd) for details.

#### <h4 id="lex_wsb_med_lda_cmd">Lex-WSB-Med-LDA: [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_cmd) with Hinge Loss</h4>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model lex-wsb-med-lda --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --label <label-file>
```

- See [(Zhu et al., 2012) and (Zhu et al., 2014)](#med_lda_ref) for hinge loss.
- Extends [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_cmd).
- Label is either 1 or -1.
- Optional arguments
	- `-c <c-value>`: Regularization parameter in hinge loss (default: 1.0). Must be a positive real number.

### <h3 id="bp_lda_cmd">BP-LDA: [LDA](#lda_cmd) with Block Priors</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model bp-lda --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --block-graph <block-graph-file>
```

- Use priors from pre-computed blocks.
- Extends [LDA](#lda_cmd).
- Semi-optional arguments
	- `--block-graph <block-graph-file>` [optional in test]: Pre-computed block file. Each line contains a block and consists of one or more documents denoted by document numbers. Document numbers are separated by space.
- Optional arguments
	- `--alpha-prime <alpha-prime-value>`: Parameter of Dirichlet prior of block distribution over topics (default: 1.0). Must be a positive real number.

### <h3 id="st_lda_cmd">ST-LDA: Single Topic [LDA](#lda_cmd)</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model st-lda --vocab <vocab-file> --corpus <corpus-file> --short-corpus <short-corpus-file> --trained-model <model-file>
```

- Implementation of [(Hong et al., 2016)](#st_lda_ref).
- Each document can only be assigned to one topic.
- Extends [LDA](#lda_cmd).
- Semi-optional arguments
	- `--short-corpus <short-corpus-file>` [at least one of `--short-corpus` and `--corpus` should be specified]: Short corpus file.
- Optional arguments
	- `--short-theta <short-theta-file>`: Short documents' background topic distribution file.
	- `--short-topic-assign <short-topic-assign-file>`: Short documents' topic assignment file.

### <h3 id="wsb_tm_cmd">WSB-TM: Weighted Stochastic Block Topic Model</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lda --model wsb-tm --vocab <vocab-file> --corpus <corpus-file> --trained-model <model-file> --wsbm-graph <wsbm-graph-file>
```

- Use priors from [WSBM](#wsbm_cmd)-computed blocks.
- Extends [LDA](#lda_cmd).
- Semi-optional arguments
	- `--wsbm-graph <wsbm-graph-file>` [optional in test]: Link file for [WSBM](#cmd) to find blocks. See [WSBM](#wsbm_cmd) for details.
- Optional arguments
	- `--alpha-prime <alpha-prime-value>`: Parameter of Dirichlet prior of block distribution over topics (default: 1.0). Must be a positive real number.
	- `-a <a-value>`: Parameter of Gamma prior for block link rates (default: 1.0). Must be a positive real number.
	- `-b <b-value>`: Parameter of Gamma prior for block link rates (default: 1.0). Must be a positive real number.
	- `-g <gamma-value>`: Parameter of Dirichlet prior for block distribution (default: 1.0). Must be a positive real number.
	- `--blocks <num-blocks>`: Number of blocks (default: 10). Must be a positive integer.
	- `--directed`: Set all edges directed (default: false).
	- `--output-wsbm <wsbm-output-file>`: File for [WSBM](#wsbm_cmd)-identified blocks. See [WSBM](#wsbm_cmd) for details.

## <h2 id="other_cmd">Other Tools in Command Line

### <h3 id="wsbm_cmd">WSBM: Weighted Stochastic Block Model</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool wsbm --nodes <num-nodes> --blocks <num-blocks> --graph <graph-file> --output <output-file>
```

- Implementation of [(Aicher et al., 2014)](#wsbm_ref).
- Find latent blocks in a network, such that nodes in the same block are densely connected and nodes in different blocks are sparsely connected.
- Required arguments
	- `<num-nodes>`: Number of nodes in the graph. Must be a positive integer.
	- `<num-blocks>`: Number of blocks. Must be a positive integer.
	- `<graph-file>`: Graph file. Each line contains an edge in the format `node-1 \t node-2 \t weight`. Node number starts from 0. `weight` must be a non-negative integer. `weight` is optional. Its default value is 1 if not specified.
	- `<output-file>`: Result file. The i-th line contains the block assignment of i-th node.
- Optional arguments
	- `--directed`: Set the edges as directed (default: undirected).
	- `-a <a-value>`: Parameter for edge rates' Gamma prior (default: 1.0). Must be a positive real number.
	- `-b <b-value>`: Parameter for edge rates' Gamma prior (default: 1.0). Must be a positive real number.
	- `-g <gamma-value>`: Parameter for block distribution's Dirichlet prior (default 1.0). Must be a positive real number.
	- `--iters <num-iters>`: Number of iterations (default: 100). Must be a positive integer.
	- `--no-verbose`: Stop printing log to console.

### <h3 id="scc_cmd">SCC: Strongly Connected Components</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool scc --nodes <num-nodes> --graph <graph-file> --output <output-file>
```

- New implementation.
- Find [strongly connected components](https://en.wikipedia.org/wiki/Strongly_connected_component) in an undirected graph. In each component, every node is reachable from any other nodes in the same component.
- Arguments
	- `<num-nodes>`: Number of nodes in the graph. Must be a positive integer.
	- `<graph-file>`: Graph file. Each line contains an edge in the format `node-1 \t node-2`. Node number starts from 0.
	- `<output-file>`: Result file. Each line contains a strongly connected component and consists of one or more nodes denoted by node numbers. Node numbers are separated by space.

### <h3 id="stoplist_cmd">Stoplist</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool stoplist --corpus <corpus-file> --output <output-file>
```

- New implementation.
- Only supports English, but can support other languages if dictionary is provided.
- Required arguments
	- `<corpus-file>`: Corpus file with stop words. Each line contains a document. Words are separated by space.
	- `<output-file>`: Corpus file without stop words. Each line contains a document. Words are separated by space.
- Optional arguments
	- `--dict <dict-file>`: Dictionary file name. Each line contains a stop word.

### <h3 id="lemmatizer_cmd">Lemmatizer</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool lemmatizer --corpus <corpus-file> --output <output-file>
```

- A re-packaging of `opennlp.tools.lemmatizer.SimpleLemmatizer`.
- Only supports English, but can support other languages if dictionary is provided.
- Required arguments
	- `<corpus-file>`: Unlemmatized corpus file. Each line contains a unlemmatized, *tokenized*, and *POS-tagged* document.
	- `<output-file>`: Lemmatized corpus file. Each line contains a lemmatized document. Words are separated by space.
- Optional arguments
	- `--dict <dict-file>`: Dictionary file name. Each line contains a rule in the format `unlemmatized-word \t POS \t lemmatized-word`.

### <h3 id="pos_tagger_cmd">POS Tagger</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool pos-tagger --corpus <corpus-file> --output <output-file>
```

- A re-packaing of `opennlp.tools.postag.POSTaggerME` (<https://opennlp.apache.org/documentation/1.6.0/manual/opennlp.html#tools.postagger>)
- Only supports English, but can support other languages if model is provided.
- Required arguments
	- `<corpus-file>`: Untagged corpus file. Each line contains a *tokenized* untagged document.
	- `<output-file>`: Tagged corpus file. Each line contains a tagged document. Each word is annotated as `word_POS`.
- Optional arguments
	- `--model <model-file>`: [Model](https://opennlp.apache.org/documentation/1.6.0/manual/opennlp.html#tools.postagger.training) file name.

### <h3 id="stemmer_cmd">Stemmer</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool stemmer --corpus <corpus-file> --output <output-file>
```

- A re-packaging of `PorterStemmer` (<http://tartarus.org/~martin/PorterStemmer/index.html>)
- Only supports English.
- Arguments
	- `<corpus-file>`: Unstemmed corpus file. Each line contains an unstemmed document. Words are separated by space.
	- `<output-file>`: Stemmed corpus file. Each line contains a stemmed document. Words are separated by space.

### <h3 id="tokenizer_cmd">Tokenizer</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool tokenizer --corpus <corpus-file> --output <output-file>
```

- A re-packaging of `opennlp.tools.tokenize.TokenizerME` (<https://opennlp.apache.org/documentation/1.6.0/manual/opennlp.html#tools.tokenizer>)
- Only supports English, but can support other languages if model is provided.
- Required arguments
	- `<corpus-file>`: Untokenized corpus file. Each line contains a untokenized document.
	- `<output-file>`: Tokenized corpus file. Each line contains a tokenized document.
- Optional arguments
	- `--model <model-file>`: [Model](<https://opennlp.apache.org/documentation/1.6.0/manual/opennlp.html#tools.tokenizer.training>) file name.

### <h3 id="corpus_converter_cmd">Corpus Converter</h3>

```
java -cp YWWTools.jar:deps.jar yang.weiwei.Tools --tool corpus-convertor {--get-vocab|--to-index|--to-word} --word-corpus <word-corpus-file> --index-corpus <index-corpus-file> --vocab <vocab-file>
```

- New implementation
- Arguments
	- `--get-vocab`, `--to-index`, `--to-word`: One of three should be selected.
		- `--get-vocab`: Collect vocabulary from `<word-corpus-file>` and write them in `<vocab-file>`.
		- `--to-index`: Convert a word corpus file `<word-corpus-file>` into an indexed corpus file `<index-corpus-file>` and write the vocabulary in `<vocab-file>`.
		- `--to-word`: Convert an indexed corpus file `<index-corpus-file>` into a word corpus file `<word-corpus-file>` given vocabulary file `<vocab-file>`.
	- `<word-corpus-file>`: Corpus file in which documents are represented by words. Each line contains a document. Words are separated by space.
	- `<index-corpus-file>`: Corpus file in which documents are represented by word indexes and frequencies. Not required when using `--get-vocab`. Each line contains a document in the following format
	
		```
		<doc-len> <word-type-1>:<frequency-1> <word-type-2>:<frequency-2> ... <word-type-n>:<frequency-n>
		```
	
		`<doc-len>` is the total number of *tokens* in this document. `<word-type-i>` denotes the i-th word in `<vocab-file>`, starting from 0. Words with zero frequency can be omitted.

	- `<vocab-file>`: Vocabulary file. Each line contains a unique word.

## <h2 id="code_examples">Use YWWTools Source Code</h2>

To integrate my code into your project, please copy `lib/` to your project root directory and 
- include `YWWTools.jar` and `deps.jar` in your project build path, OR
- add the dependencies (in `pom.xml`) to your own dependency list

Here are examples for running some algorithms in this package. For more information, please look at JavaDoc in `doc/`.

## <h2 id="lda_code">LDA Code Examples</h2>

- Classes: `yang.weiwei.lda.LDA` and `yang.weiwei.lda.LDAParam`.
- Training code example

		LDAParam param = new LDAParam("vocab_file_name"); //initialize a parameter object and set parameters as needed
		LDA ldaTrain = new LDA(param); // initialize an LDA object
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.initialize();
		ldaTrain.sample(100); // set number of iterations as needed
		ldaTrain.writeModel("model_file_name"); // optional, see test code example
		ldaTrain.writeDocTopicDist("theta_file_name"); // optional, write document-topic distribution to file
		ldaTrain.writeResult("topic_file_name", 10); // optional, write top 10 words of each topic to file
		ldaTrain.writeDocTopicCounts("topic_count_file_name") // optional, write document-topic counts to file

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		LDA ldaTest = new LDA(ldaTrain, param); // initialize with pre-trained LDA object
		// LDA ldaTest = new LDA("model_file_name", param); // or initialize with an LDA model in a file
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.initialize();
		ldaTest.sample(100); // set number of iterations as needed
		ldaTest.writeDocTopicDist("theta_file_name"); // optional, write document-topic distribution to file
		ldaTest.writeDocTopicCounts("topic_count_file_name"); // optional, write document-topic counts to file

### <h3 id="rtm_code">RTM</h3>

- Class: `yang.weiwei.lda.rtm.RTM`.
- Extends [LDA](#lda_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		RTM ldaTrain = new RTM(param);
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.readGraph("train_graph_file_name", RTM.TRAIN_GRAPH); // read train graph
		ldaTrain.readGraph("test_graph_file_name", RTM.TEST_GRAPH); // read test graph
		ldaTrain.initialize();
		ldaTrain.sample(100); 
		ldaTrain.writePred("pred_file_name"); // optional, write predicted document link probabilities to file
		ldaTrain.writeRegValues("reg_value_file_name"); // optional, write doc-doc regression values to file

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		RTM ldaTest = new RTM(ldaTrain, param);
		// RTM ldaTest = new RTM("model_file_name", param); 
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.readGraph("train_graph_file_name", RTM.TRAIN_GRAPH); // optional
		ldaTest.readGraph("test_graph_file_name", RTM.TEST_GRAPH);
		ldaTest.initialize();
		ldaTest.sample(100); 
		ldaTest.writePred("pred_file_name"); // optional, write predicted document link probabilities to file
		ldaTest.writeRegValues("reg_value_file_name"); // optional, write doc-doc regression values to file

#### <h4 id="lex_wsb_rtm_code">Lex-WSB-RTM</h4>

- Class: `yang.weiwei.lda.rtm.lex_wsb_rtm.LexWSBRTM`.
- Extends [RTM](#rtm_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		LexWSBRTM ldaTrain = new LexWSBRTM(param);
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.readGraph("train_graph_file_name", RTM.TRAIN_GRAPH); 
		ldaTrain.readGraph("test_graph_file_name", RTM.TEST_GRAPH); 
		ldaTrain.readBlockGraph("wsbm_graph_file_name"); // optional, read graph for WSBM
		ldaTrain.initialize();
		ldaTrain.sample(100); 
		ldaTrain.writeBlocks("block_file_name"); // optional, write WSBM results to file

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		LexWSBRTM ldaTest = new LexWSBRTM(ldaTrain, param);
		// LexWSBRTM ldaTest = new LexWSBRTM("model_file_name", param); 
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.readGraph("train_graph_file_name", RTM.TRAIN_GRAPH); // optional
		ldaTest.readGraph("test_graph_file_name", RTM.TEST_GRAPH);
		ldaTest.readBlockGraph("wsbm_graph_file_name"); // optional
		ldaTest.initialize();
		ldaTest.sample(100); 
		ldaTest.writeBlocks("block_file_name"); // optional

#### <h4 id="lex_wsb_med_rtm_code">Lex-WSB-Med-RTM</h4>

- Class: `yang.weiwei.lda.rtm.lex_wsb_med_rtm.LexWSBMedRTM`.
- Extends [Lex-WSB-RTM](#lex_wsb_rtm_code).
- Code examples are the same with [Lex-WSB-RTM](#lex_wsb_rtm_code).

### <h3 id="slda_code">SLDA</h3>

- Class: `yang.weiwei.lda.slda.SLDA`.
- Extends [LDA](#lda_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		SLDA ldaTrain = new SLDA(param);
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.readLabels("label_file_name"); // read label file
		ldaTrain.initialize();
		ldaTrain.sample(100);
		ldaTrain.writePredLabels("pred_label_file_name"); // optional, write predicted labels
		ldaTrain.writeRegValues("reg_value_file_name"); // optioanl, write regression values

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		SLDA ldaTest = new SLDA(ldaTrain, param);
		// SLDA ldaTest = new SLDA("model_file_name", param);
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.readLabels("label_file_name"); // optional
		ldaTest.initialize();
		ldaTest.sample(100);
		ldaTest.writePredLabels("pred_label_file_name"); // optional
		ldaTest.writeRegValues("reg_value_file_name"); // optional

#### <h4 id="bs_lda_code">BS-LDA</h4>

- Class: `yang.weiwei.lda.slda.bs_lda.BSLDA`
- Extends [SLDA](#slda_code).
- Code examples are the same with [SLDA](#slda_code).

#### <h4 id="lex_wsb_bs_lda_code">Lex-WSB-BS-LDA</h4>

- Class: `yang.weiwei.lda.slda.lex_wsb_bs_lda.LexWSBBSLDA`.
- Extends [BS-LDA](#bs_lda_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		LexWSBBSLDA ldaTrain = new LexWSBBSLDA(param);
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.readLabels("label_file_name");
		ldaTrain.readBlockGraph("wsbm_graph_file_name"); // optional, read graph for WSBM
		ldaTrain.initialize();
		ldaTrain.sample(100);
		ldaTrain.writeBlocks("block_file_name"); // optional, write WSBM results to file

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		LexWSBBSLDA ldaTest = new LexWSBBSLDA(ldaTrain, param);
		// LexWSBBSLDA ldaTest = new LexWSBBSLDA("model_file_name", param);
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.readLabels("label_file_name"); // optional
		ldaTest.readBlockGraph("wsbm_graph_file_name"); // optional
		ldaTest.initialize();
		ldaTest.sample(100);
		ldaTest.writePredLabels("pred_label_file_name"); // optional
		ldaTest.writeBlocks("block_file_name"); // optional

#### <h4 id="lex_wsb_med_code">Lex-WSB-Med-LDA</h4>

- Class: `yang.weiwei.lda.slda.lex_wsb_med_lda.LexWSBMedLDA`.
- Extends [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_code).
- Code examples are the same with [Lex-WSB-BS-LDA](#lex_wsb_bs_lda).

### <h3 id="bp_lda_code">BP-LDA</h3>

- Class: `yang.weiwei.lda.bp_lda.BPLDA`
- Extends [LDA](#lda_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		BPLDA ldaTrain = new BPLDA(param); 
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.readBlocks("block_file_name"); // read block file
		ldaTrain.initialize();
		ldaTrain.sample(100);

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		BPLDA ldaTest = new BPLDA(ldaTrain, param);
		// BPLDA ldaTest = new BPLDA("model_file_name", param);
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.readBlocks("block_file_name"); // optional
		ldaTest.initialize();
		ldaTest.sample(100); 

### <h3 id="st_lda_code">ST-LDA</h3>

- Class: `yang.weiwei.lda.st_lda.STLDA`
- Extends [LDA](#lda_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		STLDA ldaTrain = new STLDA(param);
		ldaTrain.readCorpus("long_corpus_file_name");
		ldaTrain.readShortCorpus("short_corpus_file_name");
		ldaTrain.initialize();
		ldaTrain.sample(100);
		ldaTrain.writeShortDocTopicDist("short_theta_file_name"); // optional, write short documents' topic distribution to file
		ldaTrain.writeShortDocTopicAssign("short_topic_assign_file_name"); // optional, write short documents' topic assignments to file

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		STLDA ldaTest = new STLDA(ldaTrain, param);
		// STLDA ldaTest = new STLDA("model_file_name", param);
		ldaTest.readCorpus("long_corpus_file_name");
		ldaTest.readShortCorpus("short_corpus_file_name");
		ldaTest.initialize();
		ldaTest.sample(100);
		ldaTest.writeShortDocTopicDist("short_theta_file_name"); // optional
		ldaTest.writeShortDocTopicAssign("short_topic_assign_file_name"); // optional

### <h3 id="wsb_tm_code">WSB-TM</h3>

- Class: `yang.weiwei.lda.wsb_tm.WSBTM`
- Extends [LDA](#lda_code).
- Training code example

		LDAParam param = new LDAParam("vocab_file_name");
		WSBTM ldaTrain = new WSBTM(param); 
		ldaTrain.readCorpus("corpus_file_name");
		ldaTrain.readGraph("wsbm_graph_file_name"); // read graph file
		ldaTrain.initialize();
		ldaTrain.sample(100);

- Test code example

		LDAParam param = new LDAParam("vocab_file_name");
		WSBTM ldaTest = new WSBTM(ldaTrain, param);
		// WSBTM ldaTest = new WSBTM("model_file_name", param);
		ldaTest.readCorpus("corpus_file_name");
		ldaTest.readGraph("wsbm_graph_file_name"); // optional
		ldaTest.initialize();
		ldaTest.sample(100); 

## <h2 id="other_code">Other Code Examples</h2>

### <h3 id="wsbm_code">WSBM</h3>

- Classes: `yang.weiwei.wsbm.WSBM` and `yang.weiwei.wsbm.WSBMParam`.
- Code example

		WSBMParam param = new WSBMParam(); // initialize a parameter object and set parameters as needed
		WSBM wsbm = new WSBM(param); // initialize a WSBM object with parameters
		wsbm.readGraph("graph_file_name");
		wsbm.init();
		wsbm.sample(100); // set number of iterations as needed
		wsbm.printResults();

### <h3 id="scc_code">SCC</h3>

- Class: `yang.weiwei.scc.SCC`.
- Code example

		SCC scc = new SCC(10); // initialize with number of nodes
		scc.readGraph("graph_file_name");
		scc.cluster();
		scc.writeCluster("result_file_name");
	
### <h3 id="preprocess">English Corpus Preprocessing</h3>

- Basically there are two ways to preprocess an English corpus for topic models as follows.
	- `tokenization` -> `stop words removal` -> `stemming`
	- `tokenization` -> `POS tagging` -> `lemmatization` -> `stop words removal`
- The first way is quick but with low word readability. The second one takes more time but produce better readability.
- Finally you may want to remove low (document-)frequency words, in order to accelerate topic modeling without hurting the performance.

## <h2 id="citation">Citation</h2>

- If you use [Lex-WSB-RTM](#lex_wsb_rtm_cmd) (aka LBS-RTM), [Lex-WSB-Med-RTM](#lex_wsb_med_rtm_cmd) (aka LBH-RTM), [Lex-WSB-BS-LDA](#lex_wsb_bs_lda_cmd), and/or [Lex-WSB-Med-LDA](#lex_wsb_med_lda_cmd), please cite

		@InProceedings{Yang:Boyd-Graber:Resnik-2016,
			Title = {A Discriminative Topic Model using Document Network Structure},
			Booktitle = {Association for Computational Linguistics},
			Author = {Weiwei Yang and Jordan Boyd-Graber and Philip Resnik},
			Year = {2016},
			Location = {Berlin, Germany},
		}

- If you use [ST-LDA](#st_lda_cmd), please cite

		@InProceedings{Hong:Yang:Resnik:Frias-Martinez-2016,
			Title = {Uncovering Topic Dynamics of Social Media and News: The Case of Ferguson},
			Booktitle = {International Conference on Social Informatics},
			Author = {Lingzi Hong and Weiwei Yang and Philip Resnik and Vanessa Frias-Martinez},
			Year = {2016},
			Location = {Bellevue, WA, USA}
		}

## <h2 id="ref">References</h2>

### <h3 id="lda_ref">[LDA](#lda_cmd): Latent Dirichlet Allocation</h3>

David M. Blei, Andrew Y. Ng, and Michael I. Jordan. 2003. Latent Dirichlet allocation. Journal of Machine Learning Research.

### <h3 id="slda_ref">[SLDA](#slda_cmd): Supervised [LDA](#lda_cmd)</h3>

Jon D. McAuliffe and David M. Blei. 2008. Supervised topic models. In Proceedings of Advances in Neural Information Processing Systems.

### <h3 id="med_lda_ref">Med-LDA: Max-margin [LDA](#lda_cmd)</h3>

Jun Zhu, Amr Ahmed, and Eric P. Xing. 2012. MedLDA: Maximum margin supervised topic models. Journal of Machine Learning Research.

Jun Zhu, Ning Chen, Hugh Perkins, and Bo Zhang. 2014. Gibbs max-margin topic models with data augmentation. Journal of Machine Learning Research.

### <h3 id="rtm_ref">[RTM](#rtm_cmd): Relational Topic Model</h3>

Jonathan Chang and David M. Blei. 2010. Hierarchical relational models for document networks. The Annals of Applied Statistics.

### <h3 id="lex_wsb_med_rtm_ref">[Lex-WSB-Med-RTM](#lex_wsb_med_rtm_cmd): [RTM](#rtm_cmd) with WSB-computed Block Priors, Lexical Weights, and Hinge Loss

Weiwei Yang, Jordan Boyd-Graber, and Philip Resnik. 2016. A discriminative topic model using document network structure. In Proceedings of Association for Computational Linguistics.

### <h3 id="st_lda_ref">[ST-LDA](#st_lda_cmd): Single Topic [LDA](#lda_cmd)

Lingzi Hong, Weiwei Yang, Philip Resnik, and Vanessa Frias-Martinez. 2016. Uncovering topic dynamics of social media and news: The case of Ferguson. In Proceedings of International Conference on Social Informatics.

### <h3 id="wsbm_ref">[WSBM](#wsbm_cmd): Weighted Stochastic Block Model</h3>

Christopher Aicher, Abigail Z. Jacobs, and Aaron Clauset. 2014. Learning latent block structure in weighted networks. Journal of Complex Networks.

[Back to Top](#top)