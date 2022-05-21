# LanguageTool Polish Lemmatizer plugin for OpenSearch

**WARNING: Proof of concept**

LanguageTool plugin for OpenSearch 1.x. It's languagetool lucene wrapper for OpenSearch.

Plugin provide "languagetool_pl_stem" token filter.

Inspired and initialy forked from https://github.com/allegro/elasticsearch-analysis-morfologik

## Install
  
```
bin/opensearch-plugin install pl.handslab.opensearch.plugin:opensearch-analysis-languagetool-pl:1.1.0
```

*tip: select proper plugin version, should be the same as elasticsearch version*

## Changelog:
version 1.1.0
 - initial fork

## Examples


### "languagetool_pl_stem" filter
Request:
```
GET _analyze
{
  "text": "jestem",
  "tokenizer": "keyword",
  "filter": [
		"languagetool_pl_stem"
  ]
}
```
Response:
```
{
	"tokens": [
		{
			"token": "_pos_sent_start",
			"start_offset": 0,
			"end_offset": 0,
			"type": "pos",
			"position": 0
		},
		{
			"token": "jestem",
			"start_offset": 0,
			"end_offset": 6,
			"type": "word",
			"position": 1
		},
		{
			"token": "_lemma_być",
			"start_offset": 0,
			"end_offset": 6,
			"type": "pos",
			"position": 1
		},
		{
			"token": "_pos_sent_end",
			"start_offset": 0,
			"end_offset": 6,
			"type": "pos",
			"position": 1
		},
		{
			"token": "_lemma_być",
			"start_offset": 0,
			"end_offset": 6,
			"type": "pos",
			"position": 1
		},
		{
			"token": "_pos_verb:fin:sg:pri:imperf:nonrefl",
			"start_offset": 0,
			"end_offset": 6,
			"type": "pos",
			"position": 1
		}
	]
}
```


## Supported OpenSearch versions:

### Opensearch 1.x.x
- 1.1.0


#### Install in OpenSearch for version <= 1.1.0
 
```
bin/opensearch-plugin install \
  file:///opensearch-analysis-languagetool-pl-1.1.0-plugin.zip
```

*tip: select proper plugin version, should be the same as OpenSearch version*


## Build
Build require: Java 11
`./gradlew clean buildPluginZip`

