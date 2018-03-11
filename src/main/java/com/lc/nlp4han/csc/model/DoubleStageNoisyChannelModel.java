package com.lc.nlp4han.csc.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import com.lc.nlp4han.csc.ngram.NGramModel;
import com.lc.nlp4han.csc.util.ConfusionSet;
import com.lc.nlp4han.csc.util.Dictionary;
import com.lc.nlp4han.csc.util.Sentence;
import com.lc.nlp4han.csc.wordseg.AbstractWordSegment;

/**
 *<ul>
 *<li>Description: 二阶拼写纠正器(在SIMD拼写纠正器的基础上继续使用BCWS拼写纠正器进行纠正)
 *<li>Company: HUST
 *<li>@author Sonly
 *<li>Date: 2017年11月15日
 *</ul>
 */
public class DoubleStageNoisyChannelModel extends AbstractNoisyChannelModel {
	
	private Dictionary dictionary;
	private AbstractWordSegment wordSegment;
 
	public DoubleStageNoisyChannelModel(Dictionary dictionary, NGramModel nGramModel, ConfusionSet confusionSet,
			AbstractWordSegment wordSegment) throws IOException {
		super(confusionSet, nGramModel);
		
		this.dictionary = dictionary;
		this.wordSegment = wordSegment;
	}
	
	public DoubleStageNoisyChannelModel(Dictionary dictionary, NGramModel nGramModel, ConfusionSet confusionSet,
			AbstractWordSegment wordSegment, double magicNumber) throws IOException {
		super(confusionSet, nGramModel, magicNumber);
		
		this.dictionary = dictionary;
		this.wordSegment = wordSegment;
	}
	
	@Override
	public Sentence getBestSentence(Sentence sentence) {
		return getBestKSentence(sentence, 1).get(0);
	}
	
	@Override
	public ArrayList<Sentence> getBestKSentence(Sentence sentence, int k) {
		if(k < 1)
			throw new IllegalArgumentException("返回候选句子数目不能小于1");
		beamSize = k;
		ArrayList<Sentence> candSens = new ArrayList<>();
		ArrayList<Integer> locations = new ArrayList<>();
		
		////////////////////////////////基于bigram匹配的检错
		locations = getErrorLocationsBySIMD(dictionary, sentence);
		//连续单字词的最大个数小于2，不作处理直接返回原句
		if(locations.size() > 1) {
			candSens = beamSearch(confusionSet, beamSize, sentence, locations);
			return candSens;
		}else {
			candSens.add(sentence);
		}
		
		////////////////////////////////基于分词的检错
		sentence = candSens.get(0);
		ArrayList<String> words = wordSegment.segment(sentence);
		if(words.size() < 2) {//分词后，词的个数小于2的不作处理，不作处理直接返回原句
			return candSens;
		}
		locations = locationsOfSingleWords(words);
		//连续单字词的最大个数小于2，不作处理直接返回原句
		if(locations.size() > 1) {
			candSens = new ArrayList<>();
			candSens = beamSearch(confusionSet, beamSize, sentence, locations);
			return candSens;
		}	

		return candSens;
	}

	@Override
	public double getSourceModelLogScore(Sentence candidate) {
		return nGramModel.getSentenceLogProb(candidate, order);
	}

	@Override
	public double getChannelModelLogScore(Sentence sentence, int location, String candidate, HashSet<String> cands) {
		return 1.0;
	}
}

