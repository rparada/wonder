package er.grouping;

import java.lang.*;
import java.util.*;
import java.io.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import com.webobjects.eoaccess.*;
import com.webobjects.appserver.*;
import er.extensions.*;

/* DRRecordGroup.h created by Administrator on Sun 01-Nov-1998 */
//#import <WebObjects/WebObjects.h>
public class DRRecordGroup extends Object  {
    private er.extensions.ERXLogger log = er.extensions.ERXLogger.getERXLogger(DRRecordGroup.class);

    protected DRCriteria _criteria;
    protected DRGroup _group;
    protected NSMutableDictionary _totals;
    protected NSMutableDictionary _totalsByKey;
    protected NSMutableArray _recordList;
    protected NSMutableDictionary _recordGroupDict;
    protected DRRecordGroup _parent;
    protected boolean _staleTotal;
    protected boolean _pregroupedListFound;
    protected double _total;

    // not used
    protected NSMutableDictionary _lookUpCoordinates;
    
    protected NSArray _totalList;
    protected NSArray _sortedRecordList;
    protected NSArray _rawRecordList;

    static public DRRecordGroup withCriteriaGroupParent(DRCriteria c, DRGroup grp, DRRecordGroup recGrp) {
        DRRecordGroup aVal = new DRRecordGroup();
        aVal.initWithCriteria(c, grp, recGrp);
        return aVal;
    }

    private void coordsFromRecGroupDictionary(DRRecordGroup prnt, NSMutableDictionary dict) {
        DRRecordGroup nextParent = prnt.parent();
        DRCriteria crit = prnt.criteria();
        dict.setObjectForKey(crit, crit.masterCriteria().keyDesc());
        if (nextParent != null) {
            this.coordsFromRecGroupDictionary(nextParent, dict);
        }
    }

    private NSMutableDictionary buildLookUpCoordinates() {
        NSMutableDictionary dict = new NSMutableDictionary();
        if (_criteria == null) {
            return dict;
        }
        if (_parent != null) {
            this.coordsFromRecGroupDictionary(_parent, dict);
        }
        dict.setObjectForKey(_criteria, _criteria.masterCriteria().keyDesc());
        return dict;
    }

    public DRRecordGroup initWithCriteria(DRCriteria c, DRGroup grp, DRRecordGroup recGrp) {
        _criteria = c;
        _group = grp;
        _parent = recGrp;
        _lookUpCoordinates = this.buildLookUpCoordinates();
        DRRecordGroup preexistrg = null;
        if(_group != null){
            preexistrg = _group.reportModel().recordGroupForCoordinates(_lookUpCoordinates);
    
            if (preexistrg != null) {
                _recordList = preexistrg.recordList();
                _pregroupedListFound = true;
            } else {
                _group.reportModel().registerRecordGroupWithCoordinates(this, _lookUpCoordinates);
            }
        }
        return this;
    }

    public DRRecordGroup() {
        super();
        _totals = new NSMutableDictionary();
        _recordList = new NSMutableArray();
        _recordGroupDict = new NSMutableDictionary();
        _totalsByKey = new NSMutableDictionary();
        _total = 0.0;
        _staleTotal = true;
        _pregroupedListFound = false;
        _sortedRecordList = null;
    }

    public NSMutableDictionary recordGroupDict() {
        return _recordGroupDict;
    }

    public DRCriteria criteria() {
        return _criteria;
    }

    public DRGroup group() {
        return _group;
    }

    public DRValue totalForKey(String ky) {
        return (DRValue)_totalsByKey.objectForKey(ky);
    }

    public NSDictionary totals() {
        // Loop over all DRRecords and ask each 'total-able' key for its value
        // and sum up into a dictionary of totals. keys in dict are keys into records
        // values are NSNumbers. Once computed, cache.
        //OWDebug.println(1, "_staleTotal:"+_staleTotal);

        if (_staleTotal) {
            //OWDebug.println(1, "entered: this:"+toString());
            Enumeration en = this.recordList().objectEnumerator();
            //OWDebug.println(1, "entered: this.recordList().count():"+recordList().count());
            while (en.hasMoreElements()) {
                int i = 0;
                DRRecord rec = (DRRecord)en.nextElement();
                NSArray flatlist = rec.flatValueList();
                Enumeration en2 = flatlist.objectEnumerator();
                //OWDebug.println(1, "flatlist.count():"+flatlist.count());
                while (en2.hasMoreElements()) {
                    DRValue val = (DRValue)en2.nextElement();
                    double subTot, lastTot, newTot;
                    Number indexNum = new Integer(i);
                    DRValue totalValue = (DRValue)_totals.objectForKey(indexNum);
                    //OWDebug.println(1, "totalValue:"+totalValue);
                    if (totalValue == null) {
                        if (val.shouldTotal()) {
                            totalValue = DRValue.withTotalAttribute(0, val.attribute());
                            String key = totalValue.key();
                            //OWDebug.println(1, "key:"+key);
                            _totalsByKey.setObjectForKey(totalValue, totalValue.key());
                        } else {
                            totalValue = DRValue.nullTotal();
                        }

                        _totals.setObjectForKey(totalValue, indexNum);
                    }

                    lastTot = totalValue.total();
                    //OWDebug.println(1, "lastTot:"+lastTot);
                    subTot = val.total();
                    //OWDebug.println(1, "subTot:"+subTot);
                    newTot = lastTot+subTot;
                    //OWDebug.println(1, "newTot:"+newTot);
                    totalValue.setTotal(newTot);
                    i++;
                }
            }
            _staleTotal = false;
            //OWDebug.println(1, "_totals:"+_totals);
        }

        return _totals;
    }

    public NSArray totalList() {
        if (_totalList == null) {
            int cnt = this.totals().allKeys().count();
            int i;
            NSMutableArray totList = new NSMutableArray();

            for (i = 0; i < cnt; i++) {
                totList.addObject(this.totals().objectForKey(new Integer(i)));
            }
            _totalList = new NSArray(totList);
        }

        return _totalList;
    }

    public NSArray sortedRecordList() {
        if (_sortedRecordList == null) {
            NSArray ords = null;
            if(_group != null){
                ords = _group.reportModel().orderings();
            }
            _sortedRecordList = EOSortOrdering.sortedArrayUsingKeyOrderArray(_recordList, ords);
        }

        return _sortedRecordList;
    }

    public NSArray rawRecordList() {
        if (_rawRecordList == null) {
            NSMutableArray rawRecs = new NSMutableArray();
            NSArray recs = this.sortedRecordList();
            Enumeration en = recs.objectEnumerator();

            while (en.hasMoreElements()) {
                DRRecord rec = (DRRecord)en.nextElement();
                Object rawRec = rec.rawRecord();
                rawRecs.addObject(rawRec);
            }

            _rawRecordList = new NSArray(rawRecs);
        }

        return _rawRecordList;
    }

    public NSMutableArray recordList() {
        // might sort this based on settings in DRAttributes
        return _recordList;
    }

    public boolean pregroupedListFound() {
        return _pregroupedListFound;
    }

    public NSDictionary lookUpCoordinates() {
        return _lookUpCoordinates;
    }

    public NSArray children() {
        return _recordGroupDict.allValues();
    }

    public DRRecordGroup parent() {
        return _parent;
    }

    public boolean childrenFromGroupCriteriaList(DRGroup grp) {
        //was sorted
        boolean listFound = false;
        NSArray crits = grp.criteriaList();
        Enumeration anEnum = crits.objectEnumerator();

        while (anEnum.hasMoreElements()) {
            DRCriteria crit = (DRCriteria)anEnum.nextElement();
            DRRecordGroup recGrp = DRRecordGroup.withCriteriaGroupParent(crit, grp, this);
            listFound = recGrp.pregroupedListFound();
            _recordGroupDict.setObjectForKey(recGrp, crit.keyDesc());
        }

        return listFound;
    }

    public void groupSubRecordGroupGroupLookUpDict(NSArray groupList, NSDictionary groupLookUpDict) {
        int cnt = groupList.count();

        if (cnt > 0) {
            DRMasterCriteria mc = (DRMasterCriteria)groupList.objectAtIndex(0);
            DRGroup grp = (DRGroup)groupLookUpDict.objectForKey(mc.keyDesc());

            if (!this.childrenFromGroupCriteriaList(grp)) {
                this.groupByInto(this.recordList(), grp.masterCriteria(), this.recordGroupDict());
            }

            // loop over each RecordGroup and send groupSubRecordGroup:(NSArray *)groupList
            // but only count is > 1
            Enumeration anEnum = this.children().objectEnumerator();

            while (anEnum.hasMoreElements()) {
                DRRecordGroup rg = (DRRecordGroup)anEnum.nextElement();
                NSMutableArray arr = new NSMutableArray(groupList);
                arr.removeObjectAtIndex(0);
                rg.groupSubRecordGroupGroupLookUpDict(arr, groupLookUpDict);
            }

        }

    }

    public void groupByInto(NSMutableArray recs, DRMasterCriteria amc, NSMutableDictionary recGrpDict) {
        Enumeration anEnum = recs.objectEnumerator();
        while (anEnum.hasMoreElements()) {
            DRRecord rec = (DRRecord)anEnum.nextElement();
            amc.groupRecordRecordGroupsDictGroupParent(rec, recGrpDict, this.group(), this);
        }

    }

    public String toString() {
        return ""+(super.toString())+"-lc:"+(_lookUpCoordinates)+"-"+(this.recordList().count())+"-"+(_recordGroupDict.toString());
    }

    public boolean staleTotal() {
        return _staleTotal;
    }

    public void makeStale() {
        _staleTotal = true;
        _totals.removeAllObjects();
        _totalList = null;
    }

}