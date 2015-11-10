require('fileutils')

puts "Generating fraction list at #{Dir.pwd} "

def generate()
  roots = []

  deps = collect_first_order_dependencies( File.open( "#{Dir.pwd}/target/dependencies.info" ) )

  for each in deps do
    root = {}
    root['name'] = each
    root['deps'] = collect_first_order_dependencies( load_dependencies(each) )
    roots << root
  end

  targetDir = File.join( '.', 'target' );

  FileUtils.mkdir_p( targetDir )
  
  File.open( File.join( targetDir, 'fraction-list.txt' ), 'w' ) do |f|
    for root in roots do 
      name = root['name']
      deps = root['deps']
      f.puts "#{simplify(name)} = #{filter(roots,deps).collect{|e|simplify(e)}.join(', ')}"
    end
  end
end

def filter(roots,deps) 
  return deps.select{|e| roots.collect{|r|r['name']}.include?(e) }
end

def simplify(gav) 
  parts = gav.split(':')
  return "#{parts[0]}:#{parts[1]}"
end

def collect_first_order_dependencies(input) 
  deps = []
  input.each_line do |line|
    if ( line[/\A */].size == 3 )
      parts = line.strip.split(':')
      if ( parts[0] != 'org.wildfly.swarm' )
        next
      end
      if ( parts[4] != 'compile' )
        next
      end
      deps << line.strip
    end
  end
  return deps
end

def load_dependencies(gav)
  parts = gav.strip.split(':')
  core = parts[1].gsub(/wildfly-swarm-/, '')

  path = File.join( Dir.pwd, '..', core, 'api', 'target', 'dependencies.info' )
  if ( File.exists?( path ) ) 
    return File.open( path );
  end

  path = File.join( Dir.pwd, '..', core, 'target', 'dependencies.info' )
  if ( File.exists?( path ) ) 
    return File.open( path );
  end

  puts "NOT FOUND #{core}"

end


generate()
