import fdk from '@fnproject/fdk';
import { loadString, loadFile } from 'nbb';

const { myNbbFunction } = await loadFile('fn.cljs');

console.log(myNbbFunction('FnProject'));

fdk.handle(function(input){
  let name = 'World';
  if (input.name) {
    name = input.name;
  }
  console.log('\nInside Node Hello World function');
  return {'message': myNbbFunction(name) };
});
