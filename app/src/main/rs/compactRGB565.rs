#pragma version(1)
#pragma rs java_package_name(com.selfsimilartech.fractaleye)

rs_allocation gOut;

void RS_KERNEL compact(uchar4 in, uint32_t x, uint32_t y) {

    rsSetElementAt_uchar2(gOut, in.rg, x, y);

}