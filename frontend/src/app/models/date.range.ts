import { TuiDay } from "@taiga-ui/cdk";

export enum RangeType {
    Custom = 0,
    Month,
    Year
}

export class DateRange {
    constructor(readonly name: string, readonly from: TuiDay | null, readonly to: TuiDay | null, readonly type: RangeType = RangeType.Custom) {
    }

    static all(): DateRange {
        return new DateRange("All", null, null, RangeType.Custom);
    }

    static last30(): DateRange {
        return new DateRange("Last 30 days", TuiDay.currentLocal().append({ day: -30 }), null, RangeType.Custom);
    }

    static last90(): DateRange {
        return new DateRange("Last 3 Months", TuiDay.currentLocal().append({ month: -3 }), null, RangeType.Custom);
    }

    static lastYear(): DateRange {
        const now = TuiDay.currentLocal();
        return new DateRange("Last Year", TuiDay.currentLocal().append({ year: -1 }), null, RangeType.Custom);
    }

    static month(): DateRange {
        const now = TuiDay.currentLocal();
        return new DateRange(now.toLocalNativeDate().toLocaleString('default', { month: 'long' }), new TuiDay(now.year, now.month, 1), new TuiDay(now.year, now.month, now.daysCount), RangeType.Month);
    }

    static year(): DateRange {
        const now = TuiDay.currentLocal();
        return new DateRange(now.year.toString(), new TuiDay(now.year, 0, 1), new TuiDay(now.year, 11, 31), RangeType.Year);
    }

    hasPrev(): boolean {
        return this.type == RangeType.Year || this.type == RangeType.Month;
    }

    prev(): DateRange {
        if (!this.from || !this.to) {
            return this;
        }
        if (this.type == RangeType.Year) {
            const from = this.from.append({ year: -1 });
            const to = new TuiDay(from.year, 11, 31);
            return new DateRange(from.year.toString(), from, to, RangeType.Year);
        } else if (this.type == RangeType.Month) {
            const from = this.from.append({ month: -1 });
            const to = new TuiDay(from.year, from.month, from.daysCount);
            return new DateRange(from.toLocalNativeDate().toLocaleString('default', { month: 'long' }), from, to, RangeType.Month);
        }
        return this;
    }

    hasNext(): boolean {
        return (this.type == RangeType.Year || this.type == RangeType.Month) && !!this.to && this.to.dayBefore(TuiDay.currentLocal());
    }

    next(): DateRange {
        if (!this.from || !this.to || !this.hasNext()) {
            return this;
        }
        const from = this.type == RangeType.Year ? this.from.append({ year: 1 }) : this.from.append({ month: 1 });
        let to = this.type == RangeType.Year ? new TuiDay(from.year, 11, 31) : new TuiDay(from.year, from.month, from.daysCount);
        if (to.dayAfter(TuiDay.currentLocal())) {
            to = TuiDay.currentLocal();
        }
        const name = this.type == RangeType.Year ? from.year.toString() : from.toLocalNativeDate().toLocaleString('default', { month: 'long' });
        return new DateRange(name, from, to, this.type);
    }

    same(another: DateRange): boolean {
        return this.name == another.name
            && (!this.from && !another.from || !!this.from && !!another.from && this.from.daySame(another.from))
            && (!this.to && !another.to || !!this.to && !!another.to && this.to.daySame(another.to));
    }
}