#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'vocsy_epub_viewer'
  s.version          = '2.0.0'
  s.summary          = 'A Vocsy epub reader flutter plugin project.'
  s.description      = <<-DESC
A new flutter plugin project.
                       DESC
  s.homepage         = 'https://github.com/kaushikgodhani/vocsy_epub_viewer.git'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'dudecoder' => 'kaushik64494@gmail.com' }
  s.source           = { :path => '.' }
  
  
  s.source_files = [
  'Classes/**/*',
  ]

  s.dependency 'Flutter'
  
  s.ios.deployment_target = '15.0'

  s.dependency 'RealmSwift', '3.17.3'
  s.dependency 'MenuItemKit', '3.1.3'
  s.dependency 'AEXML', '4.3.3'
  s.dependency 'FontBlaster', '4.1.0'
  s.dependency 'ZFDragableModalTransition', '0.6'
  s.dependency 'SSZipArchive', '2.1.1'

  s.preserve_paths = 'FolioReaderKit.framework'
  s.xcconfig = { 'OTHER_LDFLAGS' => '-framework FolioReaderKit' }
  s.vendored_frameworks = 'FolioReaderKit.framework'
  
  
end
