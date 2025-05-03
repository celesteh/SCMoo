MooCustomEncoder {

	*new {
		^super.newCopyArgs()
	}

	value {|item, converter|

		//"running the encoder".debug(this);
		item.notNil.if({
			item.respondsTo(\toJSON).if({
				//"toJson incoming".debug(this);
				^item.toJSON(converter)
			});
		});
		^nil
	}
}


MooCustomDecoder {

	*new {
		^super.newCopyArgs()
	}

	value {|input, converter, class, moo|

		var jsonClass;

		//"value".debug(this);

		input.isNil.if({
			//"no input".debug(this);
			//this.dumpStack;
			//this.dumpBackTrace;
			Error("No Input!!").throw;
		});

		//"input is not nil".debug(this);

		//"running the decoder, converter is %, input is %, which is a %,  class is % moo is %".format(converter, input,input.class, class, moo).debug(this);
		/*
		item.notNil.if({
		item.respondsTo(\fromJSON).if({
		"toJson incoming".debug(this);
		^item.fromJSON(converter)
		});
		});
		^nil
		*/

		input.isKindOf(Dictionary).if({
			input.atIgnoreCase("class").debug(this);
			class = class ? input.atIgnoreCase("class");
			//"class is %, a kind of %".format(class, class.class).debug(this);

		});

		//obj, converter, loadType=\parseFile
		class.notNil.if({
			jsonClass = class.asSymbol.asClass;

			jsonClass.respondsTo(\fromJSON).if({
				"Send it to the class % with converter %".format(class, converter).debug(this);
				^jsonClass.fromJSON(input, converter, moo);
			});

		});

		^nil;

	}
}

MooJSONConverter : JSONlib {

	var queue, done, inTree, >moo, converted;


	*new { |postWarnings = true, useEvent=true, customEncoder=nil, customDecoder=nil|
		^super.new.init(postWarnings, useEvent, customEncoder, customDecoder)
	}


	*convertToJSON {|object, customEncoder=nil, postWarnings=true|
		if((object.isKindOf(Dictionary) or: (object.isKindOf(SequenceableCollection))).not) {
			//Error("Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class)).warn
			"Can only convert a Dictonary/Event/Array to JSON but received %".format(object.class).warn
		};
		customEncoder = customEncoder ? MooCustomEncoder();
		//"customEncoder %".format(customEncoder).debug(this);
		^super.new.init(postWarnings, customEncoder: customEncoder).prConvertTree(object)
	}

	*convertToSC {|json, customDecoder=nil, useEvent=false, postWarnings=true, moo|
		var converter, raw;
		if(json.isKindOf(Symbol)) {
			json = json.asString;
		};

		if(json.isKindOf(String).not) {
			Error("Can only parse a String to JSON but received %".format(json.class)).throw
		};
		customDecoder = customDecoder ? MooCustomDecoder();
		converter = this.new(
			postWarnings,
			customDecoder: customDecoder,
			useEvent: useEvent
		).moo_(moo);

		//"convertToSC".debug(converter);
		raw = converter.prConvertToSC(json.parseJSON);
		^converter.restoreMoo(raw);
	}

	*parseText{ arg ...args;
		^this.convertToSC(*args);
	}

	//filePath, customDecoder=nil, useEvent=true, postWarnings=true, moo
	*parseFile {|filePath, customDecoder=nil, useEvent=true, postWarnings=true, moo|
		var converter, pre_raw, raw;

		"parseFile".debug(this);

		customDecoder = customDecoder ? MooCustomDecoder();
		converter = this.new(
			postWarnings,
			customDecoder: customDecoder,
			useEvent: useEvent
		).moo_(moo);
		//"parseFile".debug(converter);

		pre_raw = filePath.parseJSONFile;

		// this is a fuck up. Is this just a JSON string?


		//"pre_raw is %".format(pre_raw).debug(converter);
		raw = converter.prConvertToSC(pre_raw);
		//"parseFile raw %".format(raw).debug(converter);
		^converter.restoreMoo(raw);
	}



	init {|postWarnings, useEvent, customEncoder, customDecoder|

		this.postWarnings = postWarnings;
		this.useEvent = useEvent;
		this.customEncoder = customEncoder;
		this.customDecoder = customDecoder;

		queue = [];
		done = [];
		converted = [];
		inTree = false;

		//"init".debug(this);
	}


	convertToJSON{|object|

		//"convertToJson".debug(object);

		^this.prConvertToJson(object);
	}

	convertTree {|object, moo|

		var obj, id, collection;

		//"convertTree".debug(this);

		object.isKindOf(Collection).if({

			collection = object.select({|o| o.isKindOf(MooObject) });

			(collection.size > 0).if({
				(collection.size != object.size).if({
					"Mixed List! Only MooObjects are being exported".warn;
				});
				^this.prConvertTree(collection.asList);
			});

			//Not a collection containing MooObjects
			"This method is for MooObjects".warn;
			^this.prConvertToJson(object);

		});

		//object.isKindOf(SimpleNumber).if({
		(object.isKindOf(String) || object.isKindOf(Symbol)).if({
			obj = moo.at(object.asSymbol);
			obj.notNil.if({
				object = obj;
			});
		});

		object.isKindOf(MooObject).if({
			^this.prConvertTree(object, moo);
		});

		// Not a Moo object
		"This method is for MooObjects".warn;
		^this.prConvertToJson(object);
	}


	enqueue {|object|

		//"enqueue".debug(object);

		object.isKindOf(MooObject).if({
			queue = queue.add(object);
		}, {
			// something has gone wrong
			MooTypeError("Object is wrong type: %".format(object.class));
		});
	}

	queueConverted{|object|
		object.isKindOf(MooObject).if({
			converted = converted.add(object);
		}, {
			// something has gone wrong
			//MooTypeError("Object is wrong type: %".format(object.class));
		});
	}

	finish {
		var obj;

		obj = converted.pop;
		{obj.notNil}.while({
			obj.restored;
			obj = converted.pop;
		});
	}

	prConvertTree {|object, moo|

		var str = "", obj, id, prev;

		moo = moo ? Moo.default;

		//"prConvertTree".debug(this);

		(inTree.not && object.isKindOf(MooObject)).if({
			inTree = true;
			//"inTree".debug(this);
			done = [object.id];
			str = "[" + object.toJSON(this) ;

			//"queue".debug(this);
			queue.do({|item| item.postln });
			//str.debug(this);

			obj = queue.pop;//queue.removeAt(0);

			{obj.notNil}.while({
				obj.isKindOf(MooObject).if({
					id = obj.id;
				}, {
					id = obj;
					//id.isKindOf(SimpleNumber).not.if({
					(id.isKindOf(String) || id.isKindOf(Symbol)).not.if({
						//obj.dump;
						//prev.notNil.if({
						//	"prev was % %".format(prev.class, prev.name).debug(this);
						//});
						//str.debug(this);
						Error("obj is % % % %".format(id.class, id, id.value.class, id.value)).throw;
					});
					obj = moo.at(id.asSymbol);
				});

				(obj.notNil && done.includes(id).not).if({
					str = str ++ "\n," + obj.toJSON(this);
					done = done ++ id;
				});

				prev = obj;
				obj = queue.pop;
				//obj = queue.removeAt(0);
			});

			//"done with tree".debug(this);

			str = str + "]";
			//^str;
			inTree = false;
		}, {


			(inTree.not && object.isKindOf(Collection)).if({
				//"get into a tree".debug(this);
				//queue = object.select({|o| o.isKindOf(MooObject) }).asList;

				object.select({|o| o.isKindOf(MooObject) }).asList.do({|o| this.enqueue(o) });

				obj = queue.pop;//queue.removeAt(0);

				//queue.debug(this);
				//obj.dump;

				obj.notNil.if({
					//"recursion".debug(this);
					str = this.prConvertTree(obj, moo);
				}, {
					//"failed to recurr".debug(this);
					str = this.prConvertToJson(object);
				});

			} , {
				//"it's just an object".debug(this);
				str = this.prConvertToJson(object);
			});
		});

		^str;
	}

	prConvertToJson{|object|

		var val, id, ret;

		//"prConvertToJson".debug(this);
		// find the player!


		object.isKindOf(MooObject).if({
			//"MooObject % %".format(object.class, object.name).debug(this);

			id = object.id;
			done.includes(id).not.if({
				//queue = queue ++ object;
				this.enqueue(object);
			});

			^"{ \"type\": \"ref\", \"id\": % }".format(super.prConvertToJson(id));
		} , {

			//"Not MooObject %".format(object.class).debug(this);

			if(customEncoder.notNil) {
				//"customEncoder is not nil".debug(this);
				val = customEncoder.value(object, this);
				//"val is %".format(val).debug(this);

				if(val.notNil) {
					^val
				}
			};

			object.isKindOf(List).if({
				object = object.asArray;
			});

			ret = super.prConvertToJson(object);

			//"returning %".format(ret).debug(this);

			^ret;
		});

	}

	prConvertToSC { |v|
		var res, val;
		//"prConvertToSC v is %".format(v).debug(this);
		if(customDecoder.notNil) {
			//"We have a decoder".debug(this);
			//input, converter, class, moo
			//value {|input, converter, class, moo|
			val = customDecoder.value(v, this, nil,  moo);
			if(val.notNil) {
				this.queueConverted(val);
				^val
			}
		};


		^super.prConvertToSC(v);


	}

	getIDFromRef{|dict, moo|
		var type, id;

		dict.isKindOf(Dictionary).if({
			type = dict.atIgnoreCase("type");
			type.notNil.if({
				id = dict.atIgnoreCase("id");
				^id;
			});

			^nil
		});
		// wrong type, do nothing
		^dict;
	}

	restoreMoo{|obj, moo|

		var class, type, id, object;

		"restoreMoo %".format(obj).debug(this);

		obj.isKindOf(Dictionary).if({

			class = obj.atIgnoreCase("class");

			class.notNil.if({
				^this.prConvertToMoo(obj, class, moo);
			}, { // is it a reference
				//type = obj.atIgnoreCase("type");
				//type.notNil.if({
				//	id = obj.atIgnoreCase("id");
				id = this.getIDFromRef(obj, moo);
				id.notNil.if({
					object = moo.at(id);
					object.notNil.if({
						^object;
					});
					^id;
				});
			});
			// neither a class nor a reference
			"Something has gone wrong in MooJSONConverter:restoreMoo".warn;
		}, {
			obj.isKindOf(String).if({
				//"its a string %".format(obj).debug(this);
				^obj
			});
			obj.isKindOf(Collection).if({ // a dictionary is a kind of a collection!

				^obj.collect({|o| this.restoreMoo(o, moo); });
			});
		});

		^obj;
	}

	prConvertToMoo { |obj, class, moo|
		var res, val;

		"prConvertToMoo %".format(obj).debug(this);

		if(customDecoder.notNil) {

			//value {|input, converter, class, moo|
			val = customDecoder.value(obj, this, class, moo);
			if(val.notNil) {
				this.queueConverted(val);
				^val
			}
		};

		^super.prConvertToSC(obj);
	}




}


+ SharedResource {
	toJSON {|converter|
		//"toJSON".debug(this);
		^converter.convertToJSON(this.value.value)
	}
}

+ SharedCV  {
	toJSON {|converter|
		//"toJSON %".format(this.value.value).debug(this);
		^converter.convertToJSON(this.value.value)
	}
}

+ String {
	stripEnclosingQuotes {
		var start, end;
		//var reject, str = this.stripWhiteSpace;
		//reject = {|s| s.reject({|c, i| ((c == $\") && (i==0)) }); }
		//^reject.(reject.(str).reverse).reverse;
		//var start=0, end, str = this.stripWhiteSpace, quotes=[$\',$\"];
		//end = str.size-1;
		//while({ quotes.includes(str[start])},{start=start+1});
		//while({ quotes.includes(str[end])},{end=end-1});
		//^this.copyRange(start,end);
		start = 0;
		(this[0] == $\").if({ start = 1 });
		end = this.size - 1;
		(this[end] == $\").if({ end = end -1});
		^this.copyRange(start, end);
	}

	asBoolean {

		^(this.asString.compare("true", true) == 0)
	}

}

+ SharedSynthDef {
	toJSON{|converter|

		^ "{ \"class\": \"SharedSynthDef\"," +
		"\"compileString\": %, ".format(this.asCompileString) +
		"\"owner\": % }".format(converter.convertToJSON(this.owner));
	}
}

+ SharedSynthDefs {
	toJSON{|converter|
		// ok, we only want to save trusted synthdefs
		var toExport = local ++ approved;
		^ converter.convertToJSON(toExport);
	}
}


+ Dictionary {

	atIgnoreCase{ |key|

		var output, allKeys, distance, count, tie, best, firstLetter, index;

		output = this.associationAt(key.asSymbol).value;

		key = key.asString;
		this.isKindOf(IdentityDictionary).not.if({
			output = output ? this.associationAt(key).value;
			output = output ? this.associationAt(key.stripWhiteSpace).value;
		});

		output = output ? this.associationAt(key.stripWhiteSpace.asSymbol).value;

		output.isNil.if({
			allKeys = this.keys.select({|akey| akey.asString.compare(key, true) == 0 }).asList;

			// if there's just one result, we found it
			(allKeys.size == 1).if({
				//"one result".debug(this);
				output = this.associationAt(allKeys[0]).value;
				output.isNil.if({
					output = this.associationAt(allKeys[0].asSymbol).value;
				});
			});

			// if there's more than one result, pick the closest
			(allKeys.size > 1).if({
				// return the closest
				distance = Dictionary();
				allKeys.do({|akey|
					// count the differences
					count = akey.asString.count({|char, index| char != key[index]});
					tie = distance.at(count);
					tie.notNil.if({
						tie.isKindOf(Collection).if({
							tie = tie ++ akey;
						} , {
							tie = [tie, akey];
						});
						distance.put(count, tie);
					} , {
						distance = distance.put(count, akey);
					});
				});

				// sort the keys
				best = distance.keys.sort[0];
				best = distance.at(best);

				// is it a tie?
				best.isKindOf(Collection).if({

					//If one has a capitalisation in the first letter and that's the difference, it wins
					firstLetter = best.select({|akey|
						akey.asString;
						akey[0] != key[0]
					});
					// ok pick one
					(firstLetter.size > 0).if({
						index = firstLetter.choose;
					});
					// fine pick randomly
					index = best.choose

				});

				index = best;
				//index.debug(this);
				output = this.associationAt(index);
				output.key.isNil.if({
					output = this.associationAt(index.asSymbol)
				});
				//^( ? this.associationAt(index.asSymbol));
				^output.value;

			});
		});

		// we have it or it's nil
		^output
	}
}